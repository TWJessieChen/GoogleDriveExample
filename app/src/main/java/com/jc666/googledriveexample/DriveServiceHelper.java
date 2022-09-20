package com.jc666.googledriveexample;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.Pair;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A utility for performing read/write operations on Drive files via the REST API and opening a
 * file picker UI via Storage Access Framework.
 */
public class DriveServiceHelper {
    final private  String TAG = this.getClass().getSimpleName();

    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;

    private String folderIdStr = "1XQAtKFc_EcmnCd9UyUAUHcqvkWnZDWhy";

    private String fileIdStr = "1TZZAHdP6YhqUhNjKar4PexviB4VgnAry";

    public DriveServiceHelper(Drive driveService) {
        mDriveService = driveService;
    }

    /**
     * createFolder
     */
    public Task<String> createFolder() {
        return Tasks.call(mExecutor, () -> {

            File fileMetadata = new File();

            /**
             * 這邊可以指定存取Folder，
             * 但是要記錄Folder ID 才能指定存取!!!
             * */
            fileMetadata.setId(folderIdStr);

            fileMetadata.setName("JC666_Folder_File");
            fileMetadata.setMimeType("application/vnd.google-apps.folder");
            try {
                File file = mDriveService.files().create(fileMetadata)
                    .setFields("id")
                    .execute();
                Log.d(TAG,"Folder ID: " + file.getId());

                //folderIdStr = file.getId();

                return file.getId();

            } catch (GoogleJsonResponseException e) {
                /**
                 * 這裡可以判斷是否有創建Folder成功，
                 * 之後要拆成兩種方式創建，
                 * 一個是有FolderID流程的，
                 * 一個是沒有FolderID流程。
                 *
                 * 可以用Google drive error code 進行判斷!!!
                 *
                 */
                Log.d(TAG,"Unable to create folder: " + e.getDetails());
                return null;
            }

        });
    }


    /**
     * uploadToFolder
     */
    public Task<String> uploadToFolder() {
        return Tasks.call(mExecutor, () -> {

            // File's metadata.
            //File fileMetadata = new File();
            //fileMetadata.setName("jc666_photo.jpg");
            //fileMetadata.setParents(Collections.singletonList(fileIdStr));
            //java.io.File filePath = new java.io.File("files/photo.jpg");
            //FileContent mediaContent = new FileContent("image/jpeg", filePath);

            /**
             * 這邊可以指定存取檔案，
             * 但是要記錄檔案ID才能指定存取!!!
             * */
            File metadata = new File()
                .setId(fileIdStr)
                .setParents(Collections.singletonList(folderIdStr))
                .setMimeType("text/plain")
                .setName("jc666_notes.txt");

            try {
                File file = mDriveService.files().create(metadata)
                    .setFields("id, parents")
                    .execute();

                Log.d(TAG,"File ID: " + file.getId());

                //fileIdStr = file.getId();

                return file.getId();
            } catch (GoogleJsonResponseException e) {
                /**
                 * 這裡可以判斷是否有上傳檔案成功，
                 * 之後要拆成兩種方式上傳，
                 * 一個是有檔案ID流程的，
                 * 一個是沒有檔案ID流程。
                 *
                 * 可以用Google drive error code 進行判斷!!!
                 *
                 */
                Log.d(TAG,"Unable to upload file: " + e.getDetails());
                return "";
            }

        });
    }


    /**
     * uploadToFolder
     */
    public Task<String> deleteFile() {
        return Tasks.call(mExecutor, () -> {

            try {
                mDriveService.files().delete(fileIdStr).execute();

                return "Success!!!";
            } catch (IOException e) {
                Log.d(TAG,"An error occurred: " + e);
                return "";
            }

        });
    }

    /**
     * downloadFile
     */
    public Task<String> downloadFile() {

        return Tasks.call(mExecutor, () -> {

            try {
                OutputStream outputStream = new ByteArrayOutputStream();

                mDriveService.files().get(fileIdStr)
                    .executeMediaAndDownloadTo(outputStream);

                String streamData = outputStream.toString();

                Log.d(TAG,"streamData: " + streamData);

                return streamData;
            } catch (GoogleJsonResponseException e) {

                Log.d(TAG,"Unable to move file: " + e.getDetails());
                return "";
            }

        });

    }

    /**
     * UpdatesFile
     */
    public Task<String> updatesFile() {
        return Tasks.call(mExecutor, () -> {
            // Create a File containing any metadata changes.
            File metadata = new File();

            String contentTime = "JC_" + System.currentTimeMillis() + "_666";

            // Convert content to an AbstractInputStreamContent instance.
            ByteArrayContent contentStream = ByteArrayContent.fromString("text/plain", contentTime);

            // Update the metadata and contents.
            mDriveService.files().update(fileIdStr, metadata, contentStream).execute();

            return contentTime;
        });
    }

    public Task<FileList> searchFiles() {
        return Tasks.call(mExecutor, new Callable<FileList>() {
            @Override
            public FileList call() throws Exception {
                return mDriveService.files().list()
                    .setQ("mimeType='text/plain'")
                    .setSpaces("drive")
                    .execute();
            }
        });
    }

    /**
     * Creates a text file in the user's My Drive folder and returns its file ID.
     */
    public Task<String> createFile() {
        return Tasks.call(mExecutor, () -> {
            File metadata = new File()
                    .setParents(Collections.singletonList("root"))
                    .setMimeType("text/plain")
                    .setName("Untitled file");

            File googleFile = mDriveService.files().create(metadata).execute();
            if (googleFile == null) {
                throw new IOException("Null result when requesting file creation.");
            }

            return googleFile.getId();
        });
    }

    /**
     * Opens the file identified by {@code fileId} and returns a {@link Pair} of its name and
     * contents.
     */
    public Task<Pair<String, String>> readFile(String fileId) {
        return Tasks.call(mExecutor, () -> {
            // Retrieve the metadata as a File object.
            File metadata = mDriveService.files().get(fileId).execute();
            String name = metadata.getName();

            // Stream the file contents to a String.
            try (InputStream is = mDriveService.files().get(fileId).executeMediaAsInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                String contents = stringBuilder.toString();

                return Pair.create(name, contents);
            }
        });
    }

    /**
     * Updates the file identified by {@code fileId} with the given {@code name} and {@code
     * content}.
     */
    public Task<Void> saveFile(String fileId, String name, String content) {
        return Tasks.call(mExecutor, () -> {
            // Create a File containing any metadata changes.
            File metadata = new File().setName(name);

            // Convert content to an AbstractInputStreamContent instance.
            ByteArrayContent contentStream = ByteArrayContent.fromString("text/plain", content);

            // Update the metadata and contents.
            mDriveService.files().update(fileId, metadata, contentStream).execute();
            return null;
        });
    }

    /**
     * Returns a {@link FileList} containing all the visible files in the user's My Drive.
     *
     * <p>The returned list will only contain files visible to this app, i.e. those which were
     * created by this app. To perform operations on files not created by the app, the project must
     * request Drive Full Scope in the <a href="https://play.google.com/apps/publish">Google
     * Developer's Console</a> and be submitted to Google for verification.</p>
     */
    public Task<FileList> queryFiles() {
        return Tasks.call(mExecutor, new Callable<FileList>() {
            @Override
            public FileList call() throws Exception {
                return mDriveService.files().list().setSpaces("drive").execute();
            }
        });
    }

    /**
     * Returns an {@link Intent} for opening the Storage Access Framework file picker.
     */
    public Intent createFilePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");

        return intent;
    }

    /**
     * Opens the file at the {@code uri} returned by a Storage Access Framework {@link Intent}
     * created by {@link #createFilePickerIntent()} using the given {@code contentResolver}.
     */
    public Task<Pair<String, String>> openFileUsingStorageAccessFramework(
            ContentResolver contentResolver, Uri uri) {
        return Tasks.call(mExecutor, () -> {
            // Retrieve the document's display name from its metadata.
            String name;
            try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    name = cursor.getString(nameIndex);
                } else {
                    throw new IOException("Empty cursor returned for file.");
                }
            }

            // Read the document's contents as a String.
            String content;
            try (InputStream is = contentResolver.openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                content = stringBuilder.toString();
            }

            return Pair.create(name, content);
        });
    }
}
