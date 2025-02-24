//package com.saif.jobnet.Utils;
//
//
//import java.io.File;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ExecutionException;
//
//public class Main {
//    public static void main(String[] args) {
//        String url = System.getenv("SUPABASE_URL");
//        String serviceToken = System.getenv("SUPABASE_SERVICE_TOKEN");
//
//        StorageClient storageClient = new StorageClient(serviceToken, url);
//
//        // Interact with Supabase Storage
//        CompletableFuture<CreateBucketResponse> res = storageClient.createBucket("examplebucket");
//
//        // Do something on future completion.
//        res.thenAccept((bucketRes) -> {
//            IStorageFileAPI fileAPI = storageClient.from(bucketRes.getName());
//            try {
//                // We call .get here to block the thread and retrieve the value or an exception.
//                // Pass the file path in supabase storage and pass a file object of the file you want to upload.
//                FilePathResponse response = fileAPI.upload("my-secret-image/image.png", new File("file-path-to-image.png")).get();
//
//                // Generate a public url (The link is only valid if the bucket is public).
//                fileAPI.getPublicUrl("my-secret-image/image.png", new FileDownloadOption(false), new FileTransformOptions(500, 500, ResizeOption.COVER, 50, FormatOption.NONE));
//
//                // Create a signed url to download an object in a private bucket that expires in 60 seconds, and will be downloaded instantly on link as "my-image.png"
//                fileAPI.getSignedUrl("my-secret-image/image.png", 60, new FileDownloadOption("my-image.png"), null);
//
//                // Download the file
//                fileAPI.download("my-secret-image/image.png", null);
//
//            } catch (InterruptedException | ExecutionException e) {
//                throw new RuntimeException(e);
//            }
//        });
//    }
//}
