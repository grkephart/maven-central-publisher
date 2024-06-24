/**
 * 
 */
package org.drdeesw.mcp;


import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;


/**
 * 
 */
public class MavenCentralPublisher
{
  private Properties properties;

  public MavenCentralPublisher()
  {
    loadProperties();
  }


  private void loadProperties()
  {
    properties = new Properties();
    try (FileInputStream input = new FileInputStream("config.properties"))
    {
      this.properties.load(input);
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }


  public void setupProject()
  {
    createPomXml();
    createSettingsXml();
    createSonatypeAccountInstructions();
    createPgpKeyInstructions();
    createPgpKey();
  }


  private void createPomXml()
  {
    createFileFromTemplate("pom-template.xml", "target/pom.xml");
  }


  private String substituteProperties(
    String template)
  {
    for (Map.Entry<Object, Object> entry : this.properties.entrySet())
    {
      String placeholder = "{{" + entry.getKey() + "}}";
      String value = (String)entry.getValue();

      template = template.replace(placeholder, value);
    }
    
    return template;
  }


  private void createSettingsXml()
  {
    createFileFromTemplate("settings-template.xml", "target/settings.xml");
  }


  private void createFileFromTemplate(
    String templateName,
    String filename)
  {
    try
    {
      String template = new String(Files.readAllBytes(Paths.get(templateName)));
      String settingsContent = substituteProperties(template);

      writeFile(filename, settingsContent);
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }


  private void createSonatypeAccountInstructions()
  {
    String instructions = "### Sonatype Account Creation Instructions ###\n\n"
                          + "1. Go to https://issues.sonatype.org/secure/Signup!default.jspa and create an account.\n"
                          + "2. Verify your email address by following the instructions sent to your email.\n"
                          + "3. Once your account is created and verified, log in to https://issues.sonatype.org/.\n"
                          + "4. Create a new ticket requesting access to the OSSRH (OSS Repository Hosting) by following these steps:\n"
                          + "    a. Click on 'Create' in the top navigation bar.\n"
                          + "    b. Choose 'Community Support - Open Source Project Repository Hosting'.\n"
                          + "    c. Fill in the required details such as project information, group ID, and repository URL.\n"
                          + "    d. Submit the ticket and wait for approval.\n"
                          + "5. Once your ticket is approved, you will receive an email confirmation with further instructions.\n";

    writeFile("sonatype-instructions.txt", instructions);
  }


  private void createPgpKeyInstructions()
  {
    String instructions = "### PGP Key Creation Instructions ###\n\n"
                          + "1. Install GPG (GNU Privacy Guard) on your system. Here are some recommendations based on your OS:\n"
                          + "    - **Windows**: Use Gpg4win, available at https://gpg4win.org/.\n"
                          + "    - **macOS**: Use GPG Suite, available at https://gpgtools.org/.\n"
                          + "    - **Linux**: Install GPG using your package manager, e.g., `sudo apt-get install gnupg` for Debian-based systems.\n\n"
                          + "2. Generate a PGP key using the following commands in your terminal:\n"
                          + "    ```\n" + "    gpg --full-generate-key\n" + "    ```\n"
                          + "    Follow the prompts to complete the key generation process.\n\n"
                          + "3. Export your public and private keys:\n" + "    ```\n"
                          + "    gpg --armor --export your-email@example.com > public-key.asc\n"
                          + "    gpg --armor --export-secret-keys your-email@example.com > private-key.asc\n"
                          + "    ```\n"
                          + "4. Import your keys to Maven by adding the following to your `pom.xml` and `settings.xml`:\n"
                          + "    ```xml\n" + "    <plugin>\n"
                          + "        <groupId>org.apache.maven.plugins</groupId>\n"
                          + "        <artifactId>maven-gpg-plugin</artifactId>\n"
                          + "        <version>1.6</version>\n" + "        <executions>\n"
                          + "            <execution>\n"
                          + "                <id>sign-artifacts</id>\n"
                          + "                <phase>verify</phase>\n" + "                <goals>\n"
                          + "                    <goal>sign</goal>\n" + "                </goals>\n"
                          + "            </execution>\n" + "        </executions>\n"
                          + "        <configuration>\n"
                          + "            <gpgKeyname>your-key-id</gpgKeyname>\n"
                          + "            <gpgPassphrase>${env.GPG_PASSPHRASE}</gpgPassphrase>\n"
                          + "        </configuration>\n" + "    </plugin>\n" + "    ```";

    writeFile("pgp-key-instructions.txt", instructions);
  }


  private void createPgpKey()
  {
    try
    {
      String email = properties.getProperty("pgp.email");
      String passphrase = properties.getProperty("pgp.passphrase");

      ProcessBuilder pb = new ProcessBuilder("gpg", "--batch", "--passphrase", passphrase,
          "--quick-gen-key", email, "rsa2048", "sign,encrypt", "0");

      pb.inheritIO();
      Process process = pb.start();
      process.waitFor();

      System.out.println("PGP key created for email: " + email);
    }
    catch (IOException | InterruptedException e)
    {
      e.printStackTrace();
    }
  }


  private void writeFile(
    String fileName,
    String content)
  {
    try (FileWriter writer = new FileWriter(fileName))
    {
      writer.write(content);
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }


  public static void main(
    String[] args)
  {
    MavenCentralPublisher publisher = new MavenCentralPublisher();
    publisher.setupProject();
  }
}
