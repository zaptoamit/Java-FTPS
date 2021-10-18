package com.amitsachan.SpringFtpClient.controller;

import com.amitsachan.SpringFtpClient.service.FileDownloaderService;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
public class FileDownloaderController {
    @Autowired
    FileDownloaderService fileDownloaderService;

    @GetMapping (value = "/getfile", produces = MediaType.APPLICATION_JSON_VALUE)
    public void  downloadFile(@RequestParam("filePath") String filePath, HttpServletResponse response) throws Exception {
        Path path = Paths.get(filePath);
        response.addHeader("Content-disposition", "attachment;filename=" + path.getFileName());
        response.setContentType("application/octet-stream");
        InputStream inputStream = fileDownloaderService.getRemoteServerFileStream(filePath);
        IOUtils.copy(inputStream, response.getOutputStream());
        response.flushBuffer();
        inputStream.close();
        fileDownloaderService.closeFtpFileWriter();
    }


        @GetMapping(value = "downloadFile", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Object> getSteamingFile(@RequestParam("filePath") String filePath,HttpServletResponse response) throws IOException {
            Path path = Paths.get(filePath);
//            response.setContentType("application/pdf");
//            response.setHeader("Content-Disposition", "attachment; filename=\"demo.pdf\"");
            //InputStream inputStream = new FileInputStream(new File("C:\\demo-file.pdf"));
            InputStream inputStream = fileDownloaderService.getRemoteServerFileStream(filePath);


            StreamingResponseBody responseBody = outputStream -> {
                int nRead;
                byte[] data = new byte[1024];
                while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                    System.out.println("Writing some bytes..");
                    outputStream.write(data, 0, nRead);
                }
                outputStream.flush();
            };

            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + path.getFileName());
            headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
            ResponseEntity responseEntity = new ResponseEntity(HttpStatus.OK);
            responseEntity.getBody()
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(responseBody);
        }

}
