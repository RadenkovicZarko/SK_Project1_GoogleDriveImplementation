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
import com.google.common.io.Files;


import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.*;
import java.io.*;
import java.util.*;

import com.google.protobuf.Internal;
import org.apache.commons.io.FileUtils;



public class GoogleDriveStorage extends StorageSpecification{

    private String idRootFolder="";
    private Drive service;
    private static final String APPLICATION_NAME = "My project";
    private static FileDataStoreFactory DATA_STORE_FACTORY;
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static HttpTransport HTTP_TRANSPORT;
    private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE);

    public GoogleDriveStorage() {
        super();
    }

    static {
        try {
            StorageManager.registerStorage(new GoogleDriveStorage());
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in = GoogleDriveStorage.class.getResourceAsStream("client_secret.json");
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




    String retPathString(String path,boolean firstLine)
    {
        if(path.equals(""))
            return "";
        if(path.length()==1)
        {
            if(path.equals("."))
                return "";
            else
                return null;
        }
        if(path.length()>1)
        {
            //System.out.println(path);
            if(path.charAt(0)=='/')
            {
                String[] str= path.split("/+");
                StringBuilder stringBuilder=new StringBuilder();
                if(firstLine)
                    stringBuilder.append("/");
                for(int i=1;i<str.length;i++)
                {
                    if(i!=str.length-1)
                        stringBuilder.append(str[i]).append("/");
                    else
                        stringBuilder.append(str[i]);
                }
                return stringBuilder.toString();
            }
            else
                return null;
        }
        return null;
    }

    String retRootFolderID(String root) throws MyException
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

            for(String folder:str) { // /Zarko/Vlajko/a.txt
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
                        throw new MyException("Bad path");
                    }
                }
                listParentFolders=newListParentFolders;
            }
            if(idRootFolder.equals(""))
                return listParentFolders.get(listParentFolders.size()-1);
            else
                for(String p:listParentFolders)
                {
                   if(p.equals(idRootFolder))
                        return p;
                }

//            System.out.println("Ladno je stigao");
            throw new MyException("Bad path");
        }
        catch (Exception e)
        {
            throw new MyException("Bad path");
        }
    }


    String retFolderIDForPath(String path,String root)
    {
        path=retPathString(path,false);
        root=retPathString(root,false);
        if(path==null || root==null )
        {
//            System.out.println("Losa putanja");
//            return null;
            throw new MyException("Bad path");
        }
        if(path.equals(""))
        {
            if(!root.equals(""))
            {
//                System.out.println("Usaooooo");
//                System.out.println(root);
                return retRootFolderID(root);
            }
            else
                return "";

        }

        String[] str= path.split("/+");

        String rootPath=retRootFolderID(root);
        if(rootPath==null) {
//            return null;
            throw new MyException("Bad path");
        }
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
//                        System.out.println("Losa putanja");
//                        return null;
                        throw new MyException("Bad path");
                    }
                }
                listParentFolders=newListParentFolders;
            }
            return listParentFolders.get(listParentFolders.size()-1);

        }
        catch (Exception e)
        {
//            e.printStackTrace();
            throw new MyException("Bad path");
        }
//        return null;
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


    ///TODO -> MAIN

    public static void main(String[] args) throws IOException {
        StorageSpecification storageSpecification=new GoogleDriveStorage();
        GoogleDriveStorage googleDriveStorage=new GoogleDriveStorage();

        storageSpecification.setRootFolderPathInitialization(".");
        storageSpecification.createRootFolder();
        storageSpecification.createFolderOnSpecifiedPath(".","Zarko");


    }


    ///TODO -> IMPLEMENTATION OF ABSTRACT CLASSES

    //---------------------------------------------------Prvi deo---------------------------------------------------------------- RADI

    String isThereAlreadyStorage(String rootFolderPath)  throws MyException/// TEST OK
    {
        try
        {
            if(service==null)
            {
                service = getDriveService();
            }
            if(rootFolderPath.equals(""))
            {

                FileList fileList=service.files().list().setQ("trashed=false and parents in 'root'").execute();
                for(File f:fileList.getFiles())
                {
                    if(f.getName().equals("Skladiste"))
                    {

                        FileList insideFileList=service.files().list().setQ("trashed=false and parents in '"+f.getId()+"'").execute();
                        for(File f1:insideFileList.getFiles())
                        {

                            if(f1.getName().equals("configuration.txt"))
                            {

                                this.idRootFolder=f.getId();
                                return f1.getId();
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
                    if(f.getName().equals("Skladiste"))
                    {
                        FileList insideFileList=service.files().list().setQ("trashed=false and parents in '"+f.getId()+"'").execute();
                        for(File f1:insideFileList.getFiles())
                        {
                            if(f1.getName().equals("configuration.txt"))
                            {
                                return f1.getId();
                            }
                        }
                    }
                }
            }

            return null;

        }
        catch (Exception e)
        {
            throw new MyException("Error searhing is there already storage");
        }
    }

    @Override
    void createRootFolder() throws MyException{
        try {
            if(service==null)
            {
                service = getDriveService();
            }
            String rootFolderPath=retPathString(super.getRootFolderPath(),false);
//            System.out.println("Prosao1");
            if(isThereAlreadyStorage(rootFolderPath)!=null)
            {
                OutputStream outputStream = new ByteArrayOutputStream();
                service.files().get(isThereAlreadyStorage(rootFolderPath)).executeMediaAndDownloadTo(outputStream);

                String[] str= outputStream.toString().split("\n");
                super.getConfiguration().setSize(Integer.parseInt(str[0]));
                super.getConfiguration().setForbiddenExtensions(new ArrayList<>());
                super.getConfiguration().setNumberOfFilesInFolder(new HashMap<>());
                if(str.length>1) {
                    List<String> list = Arrays.asList(str[1].split(" ").clone());
                    if(!list.isEmpty())
                        super.getConfiguration().setForbiddenExtensions(list);
                }
                if(str.length>2)
                {
                    Map<String ,Integer> map=new HashMap<>();
                    int i=2;
                    while(i<str.length)
                    {
                        List<String> list1 = Arrays.asList(str[i].split(" ").clone());
                        map.put(list1.get(0),Integer.parseInt(list1.get(1)));
                        i++;
                    }
                    super.getConfiguration().setNumberOfFilesInFolder(map);
                }

                if(super.getRootFolderPath().equals(""))
                    super.setRootFolderPath("/Skladiste");
                else
                    super.setRootFolderPath(super.getRootFolderPath()+"/Skladiste");
                return;
            }
//            System.out.println("2");
            rootFolderPath=super.getRootFolderPath();
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
            fileWriter.write(super.getConfiguration().toString());
            fileWriter.close();

            File fileMetadata = new File();
            fileMetadata.setName("configuration.txt");
            fileMetadata.setParents(Collections.singletonList(folder.getId()));

            FileContent mediaContent = new FileContent("text/txt", f);
            File file=service.files().create(fileMetadata,mediaContent)
                    .setFields("id, parents")
                    .execute();
            this.idRootFolder=folder.getId();
            if(super.getRootFolderPath().equals(""))
                super.setRootFolderPath("/Skladiste");
            else
                super.setRootFolderPath(super.getRootFolderPath()+"/Skladiste");

            f.delete();
        }
        catch (Exception e)
        {
            throw new MyException("Error while creating root folder");
        }
    }   /// TEST OK

    @Override
    void setRootFolderPathInitialization(String path) throws MyException{

        try {
            String id = retFolderIDForPath(path, "");
        }
        catch (Exception e)
        {

            throw new MyException(e.getMessage());
        }
//        if(id == null)
//        {
//            return false;
//        }
        path=retPathString(path,true);
        super.setRootFolderPath(path);
    } //// TEST OK // METODA UNUTAR




    //---------------------------------------------------Drugi deo----------------------------------------------------------------

    @Override
    void createFolderOnSpecifiedPath(String path,String name) throws MyException{
        try{
            String id=retFolderIDForPath(path,super.getRootFolderPath());  // Ukoliko hoces da testiras, zadaj retFolderIDForPath(path,"")
            if(id==null)
            {
                throw new MyException("Bad path");
            }
            File folderMetadata = new File();
            folderMetadata.setName(name);
            folderMetadata.setMimeType("application/vnd.google-apps.folder");
            if(!id.equals(""))
                folderMetadata.setParents(Collections.singletonList(id));

            service.files().create(folderMetadata)
                    .setFields("id")
                    .execute();
            return;
        }
        catch (Exception e)
        {
            throw new MyException("Folder cannot be created at this path");
        }
    } ////TEST OK

    private int numberOfFiles(String path)
    {
        try{
            String id=retFolderIDForPath(path,super.getRootFolderPath());
            FileList fileList=service.files().list().setQ("mimeType != 'application/vnd.google-apps.folder' and trashed = false and parents in '"+id+"'").execute();
            int cnt=0;
            for (File f:fileList.getFiles())
            {
                cnt++;
            }
            return cnt;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return 0;
    }
    private int maxNumberOfFilesInDirectory(String path,Map<String ,Integer> map)
    {
        for(Map.Entry<String, Integer> e:map.entrySet())
        {
            if(e.getKey().equals(path))
            {
                return e.getValue();
            }
        }
        return Integer.MAX_VALUE;
    }
    private long sizeOfFolder(String path)
    {
        try {
            String idRootFoldera = retFolderIDForPath(path,super.getRootFolderPath());
            if(idRootFoldera==null)
            {
                return Integer.MAX_VALUE;
            }
            List<String> parentFolderList=new ArrayList<>();
            parentFolderList.add(idRootFoldera);
            long cnt=0;
            while(!parentFolderList.isEmpty()) {
                List<String> newParentFolderList = new ArrayList<>();
                for (String parentFolderID : parentFolderList) {
                    FileList fileList = new FileList();
                    FileList folderList = new FileList();
                    if (parentFolderID.equals("")) {
                        fileList = service.files().list().setQ("mimeType != 'application/vnd.google-apps.folder' and trashed=false and parents in 'root'").setFields("files(id, name, size,createdTime,fileExtension,modifiedTime)").execute();
                        folderList = service.files().list().setQ("mimeType = 'application/vnd.google-apps.folder' and trashed=false and parents in 'root'").execute();
                    }
                    else
                    {
                        fileList = service.files().list().setQ("mimeType != 'application/vnd.google-apps.folder' and trashed=false and parents in '"+parentFolderID+"'").setFields("files(id, name, size,createdTime,fileExtension,modifiedTime)").execute();
                        folderList = service.files().list().setQ("mimeType = 'application/vnd.google-apps.folder' and trashed=false and parents in '"+parentFolderID+"'").execute();
                    }
                    for (File f : fileList.getFiles()) {
                        cnt += f.getSize();
                    }

                    for (File f : folderList.getFiles()) {
                        newParentFolderList.add(f.getId());
                    }
                }
                parentFolderList=newParentFolderList;
            }
            return cnt;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void putFilesOnSpecifiedPath(List<String> listOfFiles, String path) throws MyException{
        StringBuilder stringBuilder=new StringBuilder();
        try {
            String id=retFolderIDForPath(path,super.getRootFolderPath());  // Ukoliko hoces da testiras, zadaj retFolderIDForPath(path,"")
            if(id==null)
            {
                throw new MyException("Bad path");
            }

            Long sizeOfPathFolder = sizeOfFolder(path);

            for(String filePath:listOfFiles)
            {
                java.io.File file=new java.io.File(filePath);
                if(!file.exists())
                {
                    stringBuilder.append("Something went wrong with "+filePath);
                    continue;
                }
                if(file.isDirectory())
                {
                    stringBuilder.append("This path is folder not for file: "+file.getName());
                    continue;
                }
                if(sizeOfPathFolder +FileUtils.sizeOf(file)>super.getConfiguration().getSize())  //Mora rekurzivno da se saberu velicine svih fajlova u folderu
                {
                    stringBuilder.append("There is no space for this file: "+file.getName());
                    continue;
                }

                if(super.getConfiguration().getForbiddenExtensions().contains("."+Files.getFileExtension(file.getName())))
                {
                    stringBuilder.append("This file has forbidden extension: "+file.getName());
                    continue;
                }
                if(numberOfFiles(path)+1>maxNumberOfFilesInDirectory(path,super.getConfiguration().getNumberOfFilesInFolder()))
                {
                    stringBuilder.append("There is limit for number of file for this folder: "+file.getName());
                    continue;
                }

                File fileMetadata = new File();
                fileMetadata.setName(file.getName());
                fileMetadata.setParents(Collections.singletonList(id));
                FileContent mediaContent = new FileContent("text/txt", file);
                service.files().create(fileMetadata,mediaContent)
                        .setFields("id, parents")
                        .execute();
            }
            if(!stringBuilder.toString().isEmpty())
            {
                throw new MyException(stringBuilder.toString());
            }
        }
        catch (Exception e)
        {
            throw new MyException("Something went wrong");
        }

    }////TEST OK

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
    }////TEST OK

    @Override
    void moveFileFromDirectoryToAnother(String pathFrom, String pathTo) throws MyException{
        try {
            String id=retFolderIDForPath(pathFrom,super.getRootFolderPath());  // Ukoliko hoces da testiras, zadaj retFolderIDForPath(path,"")
            String id2=retFolderIDForPath(pathTo,super.getRootFolderPath());
            if(id==null || id2==null)
            {
                throw new MyException("");
            }
            if(numberOfFiles(pathTo)+1>maxNumberOfFilesInDirectory(pathTo,super.getConfiguration().getNumberOfFilesInFolder()))
            {
                throw new MyException("There is limit for number of file for this folder");
            }


            File file=service.files().get(id).execute();
            File copiedFile = new File();
            copiedFile.setName(file.getName());
            copiedFile.setParents(Collections.singletonList(id2));
            service.files().copy(id,copiedFile).execute();
            service.files().delete(id).execute();
            return;
        }
        catch (Exception e)
        {
            throw new MyException("Something went wrong");
        }

    }///TEST OK

    List<String> vis=new ArrayList<>();

    void downloadWholeFolder(String id,String pathTo) throws MyException
    {
        try{
            vis.add(id);
            FileList fileList=service.files().list().setQ("mimeType != 'application/vnd.google-apps.folder' and trashed = false  and parents in '"+id+"'").setFields("files(id, name, size,createdTime,fileExtension,modifiedTime)").execute();
            FileList folderList=service.files().list().setQ("mimeType = 'application/vnd.google-apps.folder' and trashed = false  and parents in '"+id+"'").setFields("files(id, name, size,createdTime,fileExtension,modifiedTime)").execute();
            for(File f:fileList.getFiles())
            {
                downloadFile(f.getId(),pathTo,f.getSize());
            }
            for (File file:folderList.getFiles())
            {
                if(!vis.contains(file.getId()))
                {
                    java.io.File folder=new java.io.File(pathTo+"\\"+file.getName());
                    folder.mkdir();
                    downloadWholeFolder(file.getId(),pathTo+"\\"+file.getName());
                }
            }
        }
        catch (Exception e)
        {
            throw new MyException("Something went wrong");
        }
    }

    void downloadFile(String fileID,String pathTo,long size) throws MyException
    {
        try
        {
            java.io.File f1=new java.io.File(pathTo);
            if(!f1.exists())
            {
                return;
            }
            File file=service.files().get(fileID).execute();
            java.io.File f=new java.io.File(pathTo+"\\"+file.getName());
            OutputStream outputStream = new FileOutputStream(f);
            if(size>0) {
                service.files().get(fileID).executeMediaAndDownloadTo(outputStream);
                outputStream.flush();
                outputStream.close();
            }
            f.createNewFile();
        }
        catch (Exception e)
        {
            throw new MyException("Something went wrong");
        }
    }

    String getParentId(String pathTo)
    {
        try{
            String[] str=pathTo.split("/+");
            String pathToParent="";
            for(int i=0;i<str.length-1;i++)
            {
                pathToParent+=str[i];
                if(i!=str.length-2)
                    pathToParent+="/";
            }
            return retFolderIDForPath(pathToParent,super.getRootFolderPath());
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    void downloadFileOrDirectory(String pathFrom, String pathTo) throws MyException{
        try {
            String id = retFolderIDForPath(pathFrom, super.getRootFolderPath());
            if (id == null) {
                throw new MyException("Bad path");
            }
            File f = service.files().get(id).execute();
            if (f.getMimeType().equals("application/vnd.google-apps.folder")) {
                java.io.File folder = new java.io.File(pathTo + "\\" + f.getName());
                folder.mkdir();
                downloadWholeFolder(id, pathTo + "\\" + f.getName());
            }
            else
            {
                String parentID=getParentId(pathFrom);
                FileList fileList=service.files().list().setQ("mimeType != 'application/vnd.google-apps.folder' and trashed = false  and parents in '"+parentID+"'").setFields("files(id, name, size,createdTime,fileExtension,modifiedTime)").execute();
                long size=0;
                for(File f1:fileList.getFiles())
                {
                    if(f1.getName().equals(f.getName()))
                    {
                        size=f1.getSize();
                        break;
                    }
                }
                downloadFile(id,pathTo,size);
            }

        }
        catch (Exception e)
        {
            throw new MyException("Something went wrong");
        }
    } ///TEST OK


    @Override
    void renameFileOrDirectory(String path, String nameAfter) {
        try{
            String fileId=retFolderIDForPath(path,super.getRootFolderPath());
            if(fileId==null)
                return;
            File file=new File();
            file.setName(nameAfter);
            service.files().update(fileId,file).execute();
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
    } ///TEST OK

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
    }///TEST OK

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
            folderList.add(service.files().get(id).execute());
            //System.out.println(service.files().get(id).execute().getName());

            while(!folderList.isEmpty()) {
                List<File> newFolderList=new ArrayList<>();
                for (File f : folderList) {
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

    }///TEST OK


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
            if(list.contains("."+entry.getValue().getExtensions()))
            {
                retFileMap.put(entry.getKey(),entry.getValue());
            }
        }
        return retFileMap;
    }///TEST OK

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
            if(list.contains("."+entry.getValue().getExtensions()))
            {
                retFileMap.put(entry.getKey(),entry.getValue());
            }
        }
        return retFileMap;
    }///TEST OK

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
            if(list.contains("."+entry.getValue().getExtensions()))
            {
                retFileMap.put(entry.getKey(),entry.getValue());
            }
        }
        return retFileMap;
    }///TEST OK



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
    }///TEST OK

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
    }///TEST OK

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
    }///TEST OK



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
    Map<String,FileMetadata> returnCreatedFilesInDateInterval(String pathToDirectory, Date fromDate, Date toDate) throws MyException{
        try {
            String id=retFolderIDForPath(pathToDirectory,super.getRootFolderPath());  // Ukoliko hoces da testiras, zadaj retFolderIDForPath(path,"")
            if(id==null)
            {
                throw new MyException("Bad path");
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
            throw new MyException("Something went wrong");
        }

    }

    @Override
    Map<String, FileMetadata> returnModifiedFilesInDateInterval(String pathToDirectory, Date fromDate, Date toDate) throws MyException{
        try {
            String id=retFolderIDForPath(pathToDirectory,super.getRootFolderPath());  // Ukoliko hoces da testiras, zadaj retFolderIDForPath(path,"")
            if(id==null)
            {
                throw new MyException("Bad path");
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
            throw new MyException("Something went wrong");
        }
    }

    @Override
    Map<String, FileMetadata> returnModifiedFilesFromDate(String pathToDirectory, Date fromDate) throws MyException{
        try {
            String id=retFolderIDForPath(pathToDirectory,super.getRootFolderPath());  // Ukoliko hoces da testiras, zadaj retFolderIDForPath(path,"")
            if(id==null)
            {
                throw new MyException("Bad path");
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
                if(modifiedDate.after(fromDate)) {
                    FileMetadata fileMetadata = new FileMetadata(null,f.getSize(), fileDate,modifiedDate, f.getFileExtension(), f.getName());
                    map.put(f.getName(), fileMetadata);
                }
            }
            return map;
        }
        catch (Exception e)
        {
            throw new MyException("Something went wrong");
        }
    }

    @Override
    Map<String, FileMetadata> returnModifiedFilesBeforeDate(String pathToDirectory, Date toDate) throws MyException{
        try {
            String id=retFolderIDForPath(pathToDirectory,super.getRootFolderPath());  // Ukoliko hoces da testiras, zadaj retFolderIDForPath(path,"")
            if(id==null)
            {
                throw new MyException("Bad path");
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
                if(modifiedDate.before(toDate)) {
                    FileMetadata fileMetadata = new FileMetadata(null,f.getSize(), fileDate,modifiedDate, f.getFileExtension(), f.getName());
                    map.put(f.getName(), fileMetadata);
                }
            }
            return map;
        }
        catch (Exception e)
        {
            throw new MyException("Something went wrong");
        }
    }

    @Override
    void addLimitForFolder(String path, int limit) {
        try{
            String id=retFolderIDForPath("",super.getRootFolderPath());
            FileList files;
            if(!id.equals(""))
                files=service.files().list().setQ("mimeType != 'application/vnd.google-apps.folder' and trashed = false  and parents in '"+id+"'") .setFields("files(id, name, size,createdTime,fileExtension,modifiedTime)").execute();
            else
                files=service.files().list().setQ("mimeType != 'application/vnd.google-apps.folder' and trashed = false  and parents in 'root'").setFields("files(id, name, size,createdTime,fileExtension,modifiedTime)").execute();

            File configurationFile=null;
            long size=0;
            for(File f:files.getFiles())
            {
                if(f.getName().equalsIgnoreCase("configuration.txt"))
                {
                    configurationFile=f;
                    size=f.getSize();
                    break;
                }
            }
            if(configurationFile==null)
                throw new MyException("");

            File file=new File();
            file.setName(configurationFile.getName());
            OutputStream outputStream = new ByteArrayOutputStream();
            service.files().get(configurationFile.getId()).executeMediaAndDownloadTo(outputStream);
            service.files().delete(configurationFile.getId()).execute();
            java.io.File f=new java.io.File("configuration.txt");
            f.createNewFile();
            FileWriter fileWriter=new FileWriter("configuration.txt");
            fileWriter.write(outputStream.toString()+path+" "+limit+"\n");
            fileWriter.close();
            File fileMetadata = new File();
            fileMetadata.setName("configuration.txt");
            fileMetadata.setParents(Collections.singletonList(id));
            FileContent mediaContent = new FileContent("text/txt", f);
            service.files().create(fileMetadata,mediaContent)
                    .setFields("id, parents")
                    .execute();

            HashMap<String,Integer> map= (HashMap<String, Integer>)super.getConfiguration().getNumberOfFilesInFolder();
            map.put(path,limit);
            super.getConfiguration().setNumberOfFilesInFolder(map);
        }
        catch (Exception e)
        {

        }
    }


}
