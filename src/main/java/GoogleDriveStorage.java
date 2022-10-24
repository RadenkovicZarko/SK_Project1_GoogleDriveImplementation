import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;



public class GoogleDriveStorage implements StorageSpecification{

    private static final String APPLICATION_NAME = "My project";
    private static FileDataStoreFactory DATA_STORE_FACTORY;
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static HttpTransport HTTP_TRANSPORT;
    private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in = GoogleDriveStorage.class.getResourceAsStream("./client_secret.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
                clientSecrets, SCOPES).setAccessType("offline").build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        return credential;
    }

    public static Drive getDriveService() throws IOException {
        Credential credential = authorize();
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }




    public static void main(String[] args) throws IOException {
        GoogleDriveStorage googleDriveStorage=new GoogleDriveStorage();
        googleDriveStorage.createRootFolder();
    }

    
    @Override
    public void createRootFolder(){
        try {
            Drive service = getDriveService();

            File folderMetadata = new File();
            folderMetadata.setName("Root");
            folderMetadata.setMimeType("application/vnd.google-apps.folder");

            File folder = service.files().create(folderMetadata)
                    .setFields("id")
                    .execute();

            File fileMetadata = new File();
            fileMetadata.setName("configuration.txt");
            fileMetadata.setParents(Collections.singletonList(folder.getId()));

            File file = service.files().create(fileMetadata)
                    .setFields("id, parents")
                    .execute();

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    @Override
    public void createRootFolder(Configuration configuration) {

    }

    @Override
    public void createFolderOnSpecifiedPath(String s) {

    }

    @Override
    public void putFileOnSpecifiedPath(List<java.io.File> list, String s) {

    }



    @Override
    public void deleteFileOrDirectory(String s) {

    }

    @Override
    public void deleteFileOrDirectory(String s, String s1) {

    }

    @Override
    public void moveFileFromDirectoryToAnother(String s, String s1, String s2) {

    }

    @Override
    public void downloadFileOrDirectory(String s, String s1) {

    }

    @Override
    public void renameFileOrDirectory(String s, String s1, String s2) {

    }

    @Override
    public HashMap<String, FileMetadata> filesFromDirectory(String s) {
        return null;
    }

    @Override
    public HashMap<String, FileMetadata> filesFromChildrenDirectory(String s) {
        return null;
    }

    @Override
    public HashMap<String, FileMetadata> allFilesFromDirectoryAndSubdirectory(String s) {
        return null;
    }

    @Override
    public HashMap<String, String> filesFromDirectoryExt(String s, List<String> list) {
        return null;
    }

    @Override
    public HashMap<String, String> filesFromChildrenDirectoryExt(String s, List<String> list) {
        return null;
    }

    @Override
    public HashMap<String, String> allFilesFromDirectoryAndSubdirectoryExt(String s, List<String> list) {
        return null;
    }

    @Override
    public HashMap<String, String> filesFromDirectorySubstring(String s, String s1) {
        return null;
    }

    @Override
    public String folderNameByFileName(String s) {
        return null;
    }

    @Override
    public List<String> returnFilesInDateInterval(String s, Date date, Date date1) {
        return null;
    }
}
