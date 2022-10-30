import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;


import javax.sound.midi.MetaEventListener;
import java.io.*;
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



    String retRootFolderID(String root)
    {
        if(root.equals(""))
        {
            return "";
        }
        try {
            if(service == null)
            {
                service = getDriveService();
            }
            String[] str= root.split("/+");
            List<String> listParentFolders=new ArrayList<>();
            listParentFolders.add("");

            for(String folder:str) {
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



    String retFolderIDForPath(String path,String root)
    {

        if(path.equals(""))
        {
            if(!root.equals(""))
            {
                return retRootFolderID(root);
            }
            else
                return "";

        }
        String[] str= path.split("/+");
        String rootPath=retRootFolderID(root);
        if(rootPath==null)
            return null;
        List<String> listParentFolders=new ArrayList<>();
        listParentFolders.add(rootPath);
        try {
            if(service == null)
            {
                service = getDriveService();
            }

            for(String folder:str) {
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

    ArrayList<String> visited=new ArrayList<>();
    String permPath;
    void retPathToFolder(String searchId,String name,String parentId,String path)
    {
        try
        {
            if(service==null)
            {
                service=getDriveService();
            }
            visited.add(parentId);

            FileList files;
            FileList folders;
            if(!parentId.equals("")) {
                files = service.files().list().setQ("mimeType != 'application/vnd.google-apps.folder' and trashed = false  and parents in '" + parentId + "'").setFields("files(id, name, size,createdTime,fileExtension)").execute();
                folders =service.files().list().setQ("mimeType = 'application/vnd.google-apps.folder' and trashed = false  and parents in '" + parentId + "'").setFields("files(id, name, size,createdTime,fileExtension)").execute();
            }
            else {
                files = service.files().list().setQ("mimeType != 'application/vnd.google-apps.folder' and trashed = false  and parents in 'root'").setFields("files(id, name, size,createdTime,fileExtension)").execute();
                folders=service.files().list().setQ("mimeType = 'application/vnd.google-apps.folder' and trashed = false  and parents in 'root'").setFields("files(id, name, size,createdTime,fileExtension)").execute();
            }

            for(File f:files.getFiles())
            {
                //System.out.println(f.getName()+" "+f.getId());
                if(f.getId().equals(searchId))
                {
                    path+="/"+f.getName();
                    permPath=path;
                    return;
                }
            }

            Iterator<File> f=folders.getFiles().listIterator();
            while(f.hasNext())
            {
                File currFile=f.next();
                if(currFile.getId().equals(searchId))
                {
                    if(path.equals(""))
                        path+=currFile.getName();
                    else
                        path+="/"+currFile.getName();
                    permPath=path;
                    return;
                }
                if(!visited.contains(currFile.getId()) && permPath==null)
                {
                    if(path.equals(""))
                        retPathToFolder(searchId,name,currFile.getId(),path+currFile.getName());
                    else
                        retPathToFolder(searchId,name,currFile.getId(),path+"/"+currFile.getName());
                }
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }



    void printContentOfFolders()
    {
        try {
            if(service==null)
            {
                service = getDriveService();
            }
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

//        storageSpecification.deleteFileOrDirectory("Root123/Zarko/Skladiste123");

//        storageSpecification.moveFileFromDirectoryToAnother("Root123/Test1.txt","Root123/Zarko");

//        storageSpecification.downloadFileOrDirectory("Root123/unnamed.jpg","C:\\\\Users\\\\mega\\\\Radna površina");

//        storageSpecification.renameFileOrDirectory("Root123/unnamed.jpg","slika.jpg");
        GoogleDriveStorage googleDriveStorage=new GoogleDriveStorage();
        //System.out.println(googleDriveStorage.retFolderIDForPath("Root123",""));
        //storageSpecification.allFilesFromDirectoryAndSubdirectory("Root123asd");
//        Map<String,FileMetadata> mapa =storageSpecification.filesFromDirectory("Root123/Zarko/Zarko123");
//        System.out.println("--------------------------------------------------------------------");
//        Map<String,FileMetadata> m = storageSpecification.sortFilesByCreatedDate(mapa,false);
//
//        for(Map.Entry<String,FileMetadata> e:m.entrySet())
//        {
//            System.out.println(e.getKey()+" "+e.getValue().getName()+" "+e.getValue().getCreatedDate());
//        }

        //storageSpecification.renameFileOrDirectory("Test1.txt","zarko.txt");
//        googleDriveStorage.retPathToFolder("19-vlQBk6p0OylsBNg1xoVuPsfbE-BIsD","Zarko","","");
//        System.out.println(googleDriveStorage.permPath);
//        googleDriveStorage.permPath=null;
//        googleDriveStorage.visited=new ArrayList<>();
//        googleDriveStorage.retPathToFolder("14WPzQzmzDyH_-H2q5pT0vbrCffGvPYAU","Root123","","");
//        System.out.println(googleDriveStorage.permPath);
//        googleDriveStorage.permPath=null;
//        googleDriveStorage.visited=new ArrayList<>();
//        googleDriveStorage.retPathToFolder("1_BuhWL0TuiBDzMpxPwwAOyPpHRzLFXGF3FGxqBr0fL4","Test1","","");
//        System.out.println(googleDriveStorage.permPath);
//        googleDriveStorage.permPath=null;
//        googleDriveStorage.visited=new ArrayList<>();
//        googleDriveStorage.retPathToFolder("1EUbEGj6rzTfVr0d7Rtane7VnML14U8Gy","unnamed.jpg","","");
//        System.out.println(googleDriveStorage.permPath);
//        googleDriveStorage.permPath=null;
//        googleDriveStorage.visited=new ArrayList<>();
//        googleDriveStorage.retPathToFolder("1g4x0B6i7HikjylGYilDBwkJPfuHosWwD","A.docx","","");
//        System.out.println(googleDriveStorage.permPath);

//        Map<String,FileMetadata> m=storageSpecification.allFilesFromDirectoryAndSubdirectory("Root123");
//        for(Map.Entry<String,FileMetadata> e:m.entrySet())
//        {
//            System.out.println(e.getKey()+" "+e.getValue().getName()+" "+e.getValue().getAbsolutePath());
//        }





    }




    ///TODO -> IMPLEMENTATION OF ABSTRACT CLASSES

    //---------------------------------------------------Prvi deo----------------------------------------------------------------

    boolean isThereAlreadyStorage()
    {
        try
        {
            if(service==null)
            {
                service = getDriveService();
            }
            String rootFolderPath=super.getRootFolderPath();
            if(rootFolderPath.equals(""))
            {
                FileList fileList=service.files().list().setQ("trashed=false and parents in 'root'").execute();
                for(File f:fileList.getFiles())
                {
                    if(f.equals("Skladiste"))
                    {
                        FileList insideFileList=service.files().list().setQ("trashed=false and parents in '"+f.getId()+"'").execute();
                        for(File f1:insideFileList.getFiles())
                        {
                            if(f1.getName().equals("configuratio.txt"))
                            {
                                return true;
                            }
                        }
                    }
                }
            }
            else
            {
                String id=retRootFolderID(rootFolderPath);
                FileList fileList=service.files().list().setQ("trashed=false and parents in '"+id+"'").execute();
                for(File f:fileList.getFiles())
                {
                    if(f.equals("Skladiste"))
                    {
                        FileList insideFileList=service.files().list().setQ("trashed=false and parents in '"+f.getId()+"'").execute();
                        for(File f1:insideFileList.getFiles())
                        {
                            if(f1.getName().equals("configuratio.txt"))
                            {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;

        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    void createRootFolder() {
        try {
            if(service==null)
            {
                service = getDriveService();
            }
            if(!isThereAlreadyStorage())
            {
                return;
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
            service.files().create(fileMetadata,mediaContent)
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
    boolean setRootFolderPathInitialization(String path) {
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
            if(id==null)
            {
                return false;
            }
            File folderMetadata = new File();
            folderMetadata.setName(name);
            folderMetadata.setMimeType("application/vnd.google-apps.folder");
            if(!id.equals(""))
                folderMetadata.setParents(Collections.singletonList(id));

            service.files().create(folderMetadata)
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
    public boolean putFilesOnSpecifiedPath(List<String> listOfFiles, String path) {
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

                service.files().create(fileMetadata)
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
            if(id==null)
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
    boolean moveFileFromDirectoryToAnother(String pathFrom, String pathTo) {
        try {
            String id=retFolderIDForPath(pathFrom,super.getRootFolderPath());  // Ukoliko hoces da testiras, zadaj retFolderIDForPath(path,"")
            String id2=retFolderIDForPath(pathTo,super.getRootFolderPath());
            if(id==null || id2==null)
            {
                return false;
            }
            File file=service.files().get(id).execute();
            File copiedFile = new File();
            copiedFile.setName(file.getName());
            copiedFile.setParents(Collections.singletonList(id2));
            service.files().copy(id,copiedFile).execute();
            service.files().delete(id).execute();
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }

    }

    @Override
    boolean downloadFileOrDirectory(String pathFrom, String pathTo) {
        try
        {
            String id=retFolderIDForPath(pathFrom,super.getRootFolderPath());
            if(id==null)
            {
                return false;
            }
            File file=service.files().get(id).execute();
            java.io.File f=new java.io.File(pathTo+"\\"+file.getName());
            OutputStream outputStream = new FileOutputStream(f);
            service.files().get(id).executeMediaAndDownloadTo(outputStream);
            outputStream.flush();
            outputStream.close();
            f.createNewFile();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    void renameFileOrDirectory(String path, String nameAfter) {
        try
        {
            String id=retFolderIDForPath(path,super.getRootFolderPath());
            if(id==null)
            {
                return;
            }
            String[] str=path.split("/+");
            String newPath="";

            for(int i=0;i<str.length-1;i++)
            {
                newPath+=str[i];
                if(i!=str.length-2)
                    newPath+="/";
            }
            String parentId;
            if(newPath.equals(""))
            {
                parentId=retRootFolderID(super.getRootFolderPath());
            }
            else
            {
                parentId=retFolderIDForPath(newPath,super.getRootFolderPath());
            }
            if(parentId==null)
            {
                return;
            }
            File copiedFile = new File();
            copiedFile.setName(nameAfter);
            if(!parentId.equals(""))
                copiedFile.setParents(Collections.singletonList(parentId));
            service.files().copy(id,copiedFile).execute();
            service.files().delete(id).execute();

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    //----------------------------------------------------Treci deo-------------------------------------------------------

    @Override
    Map<String, FileMetadata> filesFromDirectory(String path) {
        HashMap<String, FileMetadata> hashMap=new HashMap<>();
        try
        {
            String id=retFolderIDForPath(path,super.getRootFolderPath());
            if(id==null)
            {
                return null;
            }
            if(service==null)
            {
                service=getDriveService();
            }
            FileList files=new FileList();
            System.out.println(id);
            if(!id.equals(""))
                files=service.files().list().setQ("mimeType != 'application/vnd.google-apps.folder' and trashed = false  and parents in '"+id+"'") .setFields("files(id, name, size,createdTime,fileExtension,modifiedTime)").execute();
            else
                files=service.files().list().setQ("mimeType != 'application/vnd.google-apps.folder' and trashed = false  and parents in 'root'").setFields("files(id, name, size,createdTime,fileExtension,modifiedTime)").execute();

            for(File f:files.getFiles())
            {
                java.util.Date createdDate=new java.util.Date(f.getCreatedTime().getValue());
                Date modifiedDate=new Date(f.getModifiedTime().getValue());
                retPathToFolder(f.getId(),f.getName(),"","");
                FileMetadata fileMetadata=new FileMetadata(permPath,f.getSize(),createdDate,modifiedDate,f.getFileExtension(),f.getName());
                hashMap.put(f.getName(),fileMetadata);
                permPath=null;
                visited=new ArrayList<>();
            }

            return hashMap;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    Map<String, FileMetadata> filesFromChildrenDirectory(String path) {
        HashMap<String, FileMetadata> hashMap=new HashMap<>();
        try
        {
            String id=retFolderIDForPath(path,super.getRootFolderPath());
            if(id==null)
            {
                return null;
            }
            if(service==null)
            {
                service=getDriveService();
            }

            FileList folderList;
            if(!id.equals(""))
                folderList=service.files().list().setQ("mimeType = 'application/vnd.google-apps.folder' and trashed = false and parents in '"+id+"'").execute();
            else
                folderList=service.files().list().setQ("mimeType = 'application/vnd.google-apps.folder' and trashed = false and parents in 'root'").execute();


            for(File f:folderList.getFiles())
            {
                FileList files=service.files().list().setQ("mimeType != 'application/vnd.google-apps.folder' and trashed = false and parents in '"+f.getId()+"'").setFields("files(id, name, size,createdTime,fileExtension,modifiedTime)").execute();
                //System.out.println(f.getName()+"-----------------------");
                for(File f1:files.getFiles())
                {
                    java.util.Date createdDate=new java.util.Date(f1.getCreatedTime().getValue());
                    Date modifiedDate=new Date(f1.getModifiedTime().getValue());
                    retPathToFolder(f1.getId(),f1.getName(),"","");
                    FileMetadata fileMetadata=new FileMetadata(permPath,f1.getSize(),createdDate,modifiedDate,f1.getFileExtension(),f1.getName());
                    hashMap.put(f1.getName(),fileMetadata);
                    permPath=null;
                    visited=new ArrayList<>();
                }
            }
            return hashMap;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    Map<String, FileMetadata> allFilesFromDirectoryAndSubdirectory(String path) {
        HashMap<String, FileMetadata> hashMap=new HashMap<>();
        try
        {
            String id=retFolderIDForPath(path,super.getRootFolderPath());
            if(id==null)
            {
                return null;
            }
            if(service==null)
            {
                service=getDriveService();
            }

            List<File> folderList=new ArrayList<>();
            if(!id.equals(""))
                folderList=service.files().list().setQ("trashed=false and parents in '"+id+"'").execute().getFiles();
            else
                folderList=service.files().list().setQ("trashed=false and parents in 'root'").execute().getFiles();


            while(!folderList.isEmpty()) {
                List<File> newFolderList=new ArrayList<>();
                for (File f : folderList) {

                    System.out.println("\n----------------------"+f.getName()+"-----------------------\n");
                    //Ucitavanje podataka o fajlovima u trenutnom folderu
                    FileList filesFromFolder = service.files().list().setQ("mimeType != 'application/vnd.google-apps.folder' and trashed=false and parents in '" + f.getId() + "'").setFields("files(id, name, size,createdTime,fileExtension,modifiedTime)").execute();
                    for (File f1 : filesFromFolder.getFiles()) {
                        java.util.Date createdDate=new java.util.Date(f1.getCreatedTime().getValue());
                        Date modifiedDate=new Date(f1.getModifiedTime().getValue());
                        retPathToFolder(f1.getId(),f1.getName(),"","");
                        FileMetadata fileMetadata=new FileMetadata(permPath,f1.getSize(),createdDate,modifiedDate,f1.getFileExtension(),f1.getName());
                        hashMap.put(f1.getName(),fileMetadata);
                        permPath=null;
                        visited=new ArrayList<>();
                    }

                    //Ucitavanje novih podfoldera za citanje
                    FileList foldersFromFolder = service.files().list().setQ("mimeType = 'application/vnd.google-apps.folder' and trashed=false and parents in '" + f.getId() + "'").execute();
                    newFolderList.addAll(foldersFromFolder.getFiles());
                }
                folderList=newFolderList;
            }
            return hashMap;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }

    }



    @Override
    Map<String, FileMetadata> filesFromDirectoryExt(String path, List<String> list) {
        Map<String,FileMetadata> fileMap = filesFromDirectory(path);
        if(fileMap==null)
        {
            return null;
        }
        HashMap<String, FileMetadata> retFileMap = new HashMap<>();
        for(Map.Entry<String, FileMetadata> entry : fileMap.entrySet())
        {
            if(list.contains(entry.getValue().getExtensions()))
            {
                retFileMap.put(entry.getKey(),entry.getValue());
            }
        }
        return retFileMap;
    }

    @Override
    Map<String, FileMetadata> filesFromChildrenDirectoryExt(String path, List<String> list) {
        Map<String,FileMetadata> fileMap = filesFromChildrenDirectory(path);
        if(fileMap==null)
        {
            return null;
        }
        HashMap<String, FileMetadata> retFileMap = new HashMap<>();
        for(Map.Entry<String, FileMetadata> entry : fileMap.entrySet())
        {
            if(list.contains(entry.getValue().getExtensions()))
            {
                retFileMap.put(entry.getKey(),entry.getValue());
            }
        }
        return retFileMap;
    }

    @Override
    Map<String, FileMetadata> allFilesFromDirectoryAndSubdirectoryExt(String path, List<String> list) {
        Map<String,FileMetadata> fileMap = allFilesFromDirectoryAndSubdirectory(path);
        if(fileMap==null)
        {
            return null;
        }
        HashMap<String, FileMetadata> retFileMap = new HashMap<>();
        for(Map.Entry<String, FileMetadata> entry : fileMap.entrySet())
        {
            if(list.contains(entry.getValue().getExtensions()))
            {
                retFileMap.put(entry.getKey(),entry.getValue());
            }
        }
        return retFileMap;
    }



    @Override
    Map<String, FileMetadata> filesFromDirectorySubstring(String path, String substring) {
        Map<String,FileMetadata> fileMap = filesFromDirectory(path);
        if(fileMap==null)
        {
            return null;
        }
        HashMap<String, FileMetadata> retFileMap = new HashMap<>();
        for(Map.Entry<String, FileMetadata> entry : fileMap.entrySet())
        {
            if(entry.getValue().getName().contains(substring))
            {
                retFileMap.put(entry.getKey(),entry.getValue());
            }
        }
        return retFileMap;
    }

    @Override
    Map<String, FileMetadata> filesFromChildrenDirectorySubstring(String path, String substring) {

        Map<String,FileMetadata> fileMap = filesFromChildrenDirectory(path);
        if(fileMap==null)
        {
            return null;
        }
        HashMap<String, FileMetadata> retFileMap = new HashMap<>();
        for(Map.Entry<String, FileMetadata> entry : fileMap.entrySet())
        {
            if(entry.getValue().getName().contains(substring))
            {
                retFileMap.put(entry.getKey(),entry.getValue());
            }
        }
        return retFileMap;
    }

    @Override
    Map<String, FileMetadata> filesFromDirectoryAndSubdirectorySubstring(String path, String substring) {
        Map<String,FileMetadata> fileMap = allFilesFromDirectoryAndSubdirectory(path);
        if(fileMap==null)
        {
            return null;
        }
        HashMap<String, FileMetadata> retFileMap = new HashMap<>();
        for(Map.Entry<String, FileMetadata> entry : fileMap.entrySet())
        {
            if(entry.getValue().getName().contains(substring))
            {
                retFileMap.put(entry.getKey(),entry.getValue());
            }
        }
        return retFileMap;
    }




    @Override
    String doesDirectoryContainFiles(String pathToFolder,List<String> namesOfFiles) {
        try
        {
            String id=retFolderIDForPath(pathToFolder,super.getRootFolderPath());
            if(id==null)
            {
                return null;
            }
            if(service==null)
            {
                service=getDriveService();
            }
            FileList files=new FileList();
            System.out.println(id);
            if(!id.equals(""))
                files=service.files().list().setQ("mimeType != 'application/vnd.google-apps.folder' and trashed = false  and parents in '"+id+"'") .setFields("files(id, name, size,createdTime,fileExtension)").execute();
            else
                files=service.files().list().setQ("mimeType != 'application/vnd.google-apps.folder' and trashed = false  and parents in 'root'").setFields("files(id, name, size,createdTime,fileExtension)").execute();

            String result="";
            List<String> fajlovi=new ArrayList<>();
            for(File f:files.getFiles())
            {
                if(namesOfFiles.contains(f.getName()))
                {
                    fajlovi.add(f.getName());
                }
            }

            if(!fajlovi.isEmpty())
            {
                result+="Found files are: "+fajlovi.toString();
            }
            else
            {
                result+="No files exist";
            }
            return result;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    String folderNameByFileName(String fileName) {
        try
        {
            String id=retRootFolderID(super.getRootFolderPath());
            if(id==null)
            {
                return null;
            }
            if(service==null)
            {
                service=getDriveService();
            }
            FileList folders=new FileList();
            FileList files=new FileList();

            List<String> parentFolderList=new ArrayList<>();
            parentFolderList.add(id);

            while(!parentFolderList.isEmpty())
            {
                List<String> newParentFolderList=new ArrayList<>();
                for(String folderId:parentFolderList)
                {
                    if(!folderId.equals("")) {
                        files = service.files().list().setQ("mimeType != 'application/vnd.google-apps.folder' and trashed = false  and parents in '" + folderId + "'").setFields("files(id, name, size,createdTime,fileExtension,modifiedTime)").execute();
                        folders =service.files().list().setQ("mimeType = 'application/vnd.google-apps.folder' and trashed = false  and parents in '" + folderId + "'").setFields("files(id, name, size,createdTime,fileExtension,modifiedTime)").execute();
                    }
                    else {
                        files = service.files().list().setQ("mimeType != 'application/vnd.google-apps.folder' and trashed = false  and parents in 'root'").setFields("files(id, name, size,createdTime,fileExtension,modifiedTime)").execute();
                        folders=service.files().list().setQ("mimeType = 'application/vnd.google-apps.folder' and trashed = false  and parents in 'root'").setFields("files(id, name, size,createdTime,fileExtension,modifiedTime)").execute();
                    }

                    for(File f:files.getFiles())
                    {
                        if(f.getName().equals(fileName))
                        {
                            if(folderId.equals(""))
                            {
                                return "Skladiste";
                            }
                            else
                            {
                                return service.files().get(folderId).execute().getName();
                            }
                        }
                    }
                    for (File f:folders.getFiles())
                    {
                        newParentFolderList.add(f.getId());
                    }
                }
                parentFolderList=newParentFolderList;
            }
            return null;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    Map<String, FileMetadata> sortFilesByName(Map<String, FileMetadata> hashMap, boolean b) {
        ArrayList arrayList = new ArrayList<>();

        for( Object entry : hashMap.entrySet())
        {
            arrayList.add((Map.Entry)entry);
        }
        Collections.sort(arrayList, new Comparator<Map.Entry>(){
            @Override
            public int compare(Map.Entry o1, Map.Entry o2) {
                FileMetadata fm1=(FileMetadata) o1.getValue();
                FileMetadata fm2=(FileMetadata) o2.getValue();
                if(b==true)
                    return fm1.getName().compareTo(fm2.getName());
                else
                    return fm2.getName().compareTo(fm1.getName());
            }
        });

        Map sortedMap = new LinkedHashMap<>();

        for( Object entry : arrayList)
        {
            Map.Entry e=(Map.Entry)entry;
            sortedMap.put(e.getKey(),e.getValue());
        }
        return sortedMap;
    }

    @Override
    Map<String, FileMetadata> sortFilesByCreatedDate(Map<String, FileMetadata> hashMap, boolean b) {
        ArrayList arrayList = new ArrayList<>();

        for( Object entry : hashMap.entrySet())
        {
            arrayList.add((Map.Entry)entry);
        }
        Collections.sort(arrayList, new Comparator<Map.Entry>(){
            @Override
            public int compare(Map.Entry o1, Map.Entry o2) {
                FileMetadata fm1=(FileMetadata) o1.getValue();
                FileMetadata fm2=(FileMetadata) o2.getValue();
                if(b==true)
                    return fm2.getCreatedDate().compareTo(fm1.getCreatedDate());
                else
                    return fm1.getCreatedDate().compareTo(fm2.getCreatedDate());
            }
        });

        Map sortedMap = new LinkedHashMap<>();

        for( Object entry : arrayList)
        {
            Map.Entry e=(Map.Entry)entry;
            sortedMap.put(e.getKey(),e.getValue());
        }
        return sortedMap;
    }

    @Override
    Map<String, FileMetadata> sortFilesBySize(Map<String, FileMetadata> hashMap, boolean b) {
        ArrayList arrayList = new ArrayList<>();

        for( Object entry : hashMap.entrySet())
        {
            arrayList.add((Map.Entry)entry);
        }
        Collections.sort(arrayList, new Comparator<Map.Entry>(){
            @Override
            public int compare(Map.Entry o1, Map.Entry o2) {
                FileMetadata fm1=(FileMetadata) o1.getValue();
                FileMetadata fm2=(FileMetadata) o2.getValue();
                if(b==true)
                    return Long.compare(fm1.getSize(),fm2.getSize());
                else
                    return Long.compare(fm2.getSize(),fm1.getSize());
            }
        });

        Map sortedMap = new LinkedHashMap<>();

        for( Object entry : arrayList)
        {
            Map.Entry e=(Map.Entry)entry;
            sortedMap.put(e.getKey(),e.getValue());
        }
        return sortedMap;
    }

    @Override
    Map<String,FileMetadata> returnCreatedFilesInDateInterval(String pathToDirectory, Date fromDate, Date toDate) {
        try {
            String id=retFolderIDForPath(pathToDirectory,super.getRootFolderPath());  // Ukoliko hoces da testiras, zadaj retFolderIDForPath(path,"")
            if(id==null)
            {
                return null;
            }
            Map<String, FileMetadata> map=new LinkedHashMap<>();
            FileList files;
            if(!id.equals(""))
                files=service.files().list().setQ("mimeType != 'application/vnd.google-apps.folder' and trashed = false  and parents in '"+id+"'") .setFields("files(id, name, size,createdTime,fileExtension,modifiedTime)").execute();
            else
                files=service.files().list().setQ("mimeType != 'application/vnd.google-apps.folder' and trashed = false  and parents in 'root'").setFields("files(id, name, size,createdTime,fileExtension,modifiedTime)").execute();

            for(File f:files.getFiles())
            {
                java.util.Date fileDate=new java.util.Date(f.getCreatedTime().getValue());
                Date modifiedDate=new Date(f.getModifiedTime().getValue());
                if(fileDate.after(fromDate) && fileDate.before(toDate)) {
                    FileMetadata fileMetadata = new FileMetadata(null,f.getSize(), fileDate,modifiedDate, f.getFileExtension(), f.getName());
                    map.put(f.getName(), fileMetadata);
                }
            }
            return map;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }

    }

    @Override
    Map<String, FileMetadata> returnModifiedFilesInDateInterval(String pathToDirectory, Date fromDate, Date toDate) {
        try {
            String id=retFolderIDForPath(pathToDirectory,super.getRootFolderPath());  // Ukoliko hoces da testiras, zadaj retFolderIDForPath(path,"")
            if(id==null)
            {
                return null;
            }
            Map<String, FileMetadata> map=new LinkedHashMap<>();
            FileList files;
            if(!id.equals(""))
                files=service.files().list().setQ("mimeType != 'application/vnd.google-apps.folder' and trashed = false  and parents in '"+id+"'") .setFields("files(id, name, size,createdTime,fileExtension,modifiedTime)").execute();
            else
                files=service.files().list().setQ("mimeType != 'application/vnd.google-apps.folder' and trashed = false  and parents in 'root'").setFields("files(id, name, size,createdTime,fileExtension,modifiedTime)").execute();

            for(File f:files.getFiles())
            {
                java.util.Date fileDate=new java.util.Date(f.getCreatedTime().getValue());
                Date modifiedDate=new Date(f.getModifiedTime().getValue());
                if(modifiedDate.after(fromDate) && modifiedDate.before(toDate)) {
                    FileMetadata fileMetadata = new FileMetadata(null,f.getSize(), fileDate,modifiedDate, f.getFileExtension(), f.getName());
                    map.put(f.getName(), fileMetadata);
                }
            }
            return map;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }


}
