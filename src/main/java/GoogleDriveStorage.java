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


import java.io.*;
import java.nio.file.Files;
import java.util.*;


public class GoogleDriveStorage extends StorageSpecification{

    private Drive service;
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
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("anyone");
        return credential;
    }

    public static Drive getDriveService() throws IOException {
        Credential credential = authorize();
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }



    String retFolderIDForPath(String path,String root)
    {

        if(path.equals(""))
        {
            if(!root.equals(""))
            {
                return root;
            }
            else
                return "";

        }
        String id=null;
        String[] str= path.split("/+");
        List<String> listParentFolders=new ArrayList<>();
        listParentFolders.add(root);
        try {
            if(service == null)
            {
                service = getDriveService();
            }

            for(String folder:str) {
                boolean pom = false;
                List<String> newListParentFolders = new ArrayList<>();
                for (String parentFolder : listParentFolders) {
                    FileList result;
                    if(!parentFolder.equals("")) {
                        result = service.files().list()
                                .setQ("trashed = false and parents in '" + parentFolder + "'")
                                .setFields("nextPageToken, files(id, name)")
                                .execute();
                    }
                    else
                    {
                        result = service.files().list()
                                .setQ("trashed = false and parents in 'root'")
                                .setFields("nextPageToken, files(id, name)")
                                .execute();
                    }
                    for (File f : result.getFiles()) {
                        if (f.getName().equals(folder)) {
                            newListParentFolders.add(f.getId());
                        }
                    }

                    if (newListParentFolders.isEmpty()) {
                        System.out.println("Losa putanja");
                        return null;
                    }
                }
                listParentFolders=newListParentFolders;
            }

            return listParentFolders.get(listParentFolders.size()-1);

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    void printContentOfFolders()
    {
        try {
            if(service==null)
            {
                service = getDriveService();
            }
            List<File> files = new ArrayList<File>();


            FileList result = service.files().list()
                    .setQ("mimeType = 'application/vnd.google-apps.folder' and trashed=false and parents in 'root'")
                    .setFields(" files(id, name)")
                    .execute();

            for (File file : result.getFiles()) {
                System.out.printf("Found file: %s (%s)\n",
                        file.getName(), file.getId());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }



    ///TODO -> MAIN

    public static void main(String[] args) throws IOException {
        //GoogleDriveStorage googleDriveStorage=new GoogleDriveStorage();
        StorageSpecification storageSpecification=new GoogleDriveStorage();
        //googleDriveStorage.printContentOfFolders();
        /*System.out.println("Dobrodosli u nas program!\nOdaberite opciju:\n1.Lokalnog skladista\n2.Google Drive skladista");
        Scanner sc=new Scanner(System.in);
        String input=sc.nextLine();

        System.out.println("Da li zelite da uneste putanju na kojoj ce se nalaziti skladiste ili birate korenski folder:\n1.Korenski\n2.Unos putanje");
        input=sc.nextLine();
        String path="";
        if(input.equals("2"))
        {
            while(!storageSpecification.setRootFolderPathImplementation(sc.nextLine())){
                System.out.println("Unesi ponovo:");
            }
            System.out.println("Odabrana putanja je ok");
        }

        System.out.println("Da li zelite da zadate velicinu skladista:\n1.Ne\n2.Da");
        input=sc.nextLine();
        if(input.equals("2"))
        {
            input=sc.nextLine();
            storageSpecification.setConfigurationSizeOfStorage(Integer.parseInt(input));
        }

        System.out.println("Da li zelite da zadate broj fajlova u skladistu:\n1.Ne\n2.Da");
        input=sc.nextLine();
        if(input.equals("2"))
        {
            input=sc.nextLine();
            storageSpecification.setConfigurationNumberOfFiles(Integer.parseInt(input));
        }

        System.out.println("Da li zelite da zadate dozvoljene ekstenzije:\n1.Da\n2.Ne");
        input=sc.nextLine();
        if(input.equals("2"))
        {
            System.out.println("Ekstencije zadajte sa razmakom i u obliku .ekstenzija -> primer: (.exe .pdf)");
            input=sc.nextLine();
            storageSpecification.setConfigurationNumberOfFiles(Integer.parseInt(input));
        }*/
//        List<String> list=new ArrayList<>();
//        list.add("C:\\xampp\\htdocs\\ZadatakSubota");
//        list.add("C:\\Users\\mega\\Radna površina\\Test1.txt");
//        list.add("C:\\Users\\mega\\Radna površina\\ProjekatSK\\Test2.txt");
//        storageSpecification.putFileOnSpecifiedPath(list,"Root123");
//        storageSpecification.createFolderOnSpecifiedPath("Root123/Zarko","Zarko123");
//        googleDriveStorage.moveFileFromDirectoryToAnother("z.txt","Root123/Zarko/Zarko123","Root123/Zarko/Radenkovic");

        storageSpecification.deleteFileOrDirectory("Root123/");



    }




    ///TODO -> IMPLEMENTATION OF ABSTRACT CLASSES

    //--------------------------------------------------------Prvi deo----------------------------------------------------------
    @Override
    void createRootFolder() {
        try {
            if(service==null)
            {
                service = getDriveService();
            }
            String rootFolderPath=super.getRootFolderPath();
            File folderMetadata = new File();
            folderMetadata.setName("Skladiste");
            folderMetadata.setMimeType("application/vnd.google-apps.folder");
            if(!rootFolderPath.equals("")) {
                String rootFolderID=retFolderIDForPath(rootFolderPath,"");
                folderMetadata.setParents(Collections.singletonList(rootFolderID));
            }
            File folder = service.files().create(folderMetadata)
                    .setFields("id")
                    .execute();

            java.io.File f=new java.io.File("src/main/resources/configuration.txt");
            f.createNewFile();
            FileWriter fileWriter=new FileWriter("src/main/resources/configuration.txt");
            fileWriter.write(super.getConfiguration().getNumberOfFiles()+"\n"+
                    super.getConfiguration().getSize()+"\n"+super.getConfiguration().getAllowedExtensions());
            fileWriter.close();

            File fileMetadata = new File();
            fileMetadata.setName("configuration.txt");
            fileMetadata.setParents(Collections.singletonList(folder.getId()));

            FileContent mediaContent = new FileContent("text/txt", f);
            File file = service.files().create(fileMetadata,mediaContent)
                    .setFields("id, parents")
                    .execute();

            f.delete();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    boolean setRootFolderPathImplementation(String path) {
        String id=retFolderIDForPath(path,"");
        if(id == null)
        {
            return false;
        }
        super.setRootFolderPath(path);
        return true;
    }


    //---------------------------------------------------Drugi deo----------------------------------------------------------------
    @Override
    boolean createFolderOnSpecifiedPath(String path,String name) {
        try{
            String id=retFolderIDForPath(path,super.getRootFolderPath());  // Ukoliko hoces da testiras, zadaj retFolderIDForPath(path,"")
            if(id.equals(null))
            {
                return false;
            }
            File folderMetadata = new File();
            folderMetadata.setName(name);
            folderMetadata.setMimeType("application/vnd.google-apps.folder");
            if(!id.equals(""))
                folderMetadata.setParents(Collections.singletonList(id));

            File folder = service.files().create(folderMetadata)
                    .setFields("id")
                    .execute();
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean putFileOnSpecifiedPath(List<String> listOfFiles, String path) {
        try {
            String id=retFolderIDForPath(path,super.getRootFolderPath());  // Ukoliko hoces da testiras, zadaj retFolderIDForPath(path,"")
            if(id==null)
            {
                return false;
            }

            for(String filePath:listOfFiles)
            {
                java.io.File file=new java.io.File(filePath);
                if(!file.exists() || file.isDirectory())
                    continue;

                File fileMetadata = new File();
                fileMetadata.setName(file.getName());
                fileMetadata.setParents(Collections.singletonList(id));

                File f = service.files().create(fileMetadata)
                        .setFields("id, parents")
                        .execute();
            }
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }


    @Override
    void deleteFileOrDirectory(String path) {
        try {
            String id=retFolderIDForPath(path,super.getRootFolderPath());  // Ukoliko hoces da testiras, zadaj retFolderIDForPath(path,"")
            if(id.equals(null))
            {
                return;
            }
            service.files().delete(id).execute();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return;
    }

    @Override
    void moveFileFromDirectoryToAnother(String pathFrom, String pathTo) {
        try {
//            String id=retFolderIDForPath(pathFrom,super.getRootFolderPath());  // Ukoliko hoces da testiras, zadaj retFolderIDForPath(path,"")
//            String id2=retFolderIDForPath(pathTo,super.getRootFolderPath());
//            if(id.equals(null))
//            {
//                return;
//            }
//
//            FileList result = service.files().list()
//                    .setQ("parents in '" + id + "'")
//                    .setFields("files(id, name)")
//                    .execute();
//
//            for(File f:result.getFiles())
//            {
//                if(f.getName().equals(fileName))
//                {
//                    f.setWritersCanShare(true);
//                    service.files().update(f.getId(),f).setAddParents(id2).setRemoveParents(id).execute();
//
//                    return;
//                }
//            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return;
    }

    @Override
    void downloadFileOrDirectory(String s, String s1) {

    }

    @Override
    void renameFileOrDirectory(String s, String s1, String s2) {

    }

    @Override
    HashMap<String, FileMetadata> filesFromDirectory(String s) {
        return null;
    }

    @Override
    HashMap<String, FileMetadata> filesFromChildrenDirectory(String s) {
        return null;
    }

    @Override
    HashMap<String, FileMetadata> allFilesFromDirectoryAndSubdirectory(String s) {
        return null;
    }

    @Override
    HashMap<String, String> filesFromDirectoryExt(String s, List<String> list) {
        return null;
    }

    @Override
    HashMap<String, String> filesFromChildrenDirectoryExt(String s, List<String> list) {
        return null;
    }

    @Override
    HashMap<String, String> allFilesFromDirectoryAndSubdirectoryExt(String s, List<String> list) {
        return null;
    }

    @Override
    HashMap<String, String> filesFromDirectorySubstring(String s, String s1) {
        return null;
    }

    @Override
    String folderNameByFileName(String s) {
        return null;
    }

    @Override
    List<String> returnFilesInDateInterval(String s, Date date, Date date1) {
        return null;
    }


}
