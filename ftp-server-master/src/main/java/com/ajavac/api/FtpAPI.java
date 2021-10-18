package com.ajavac.api;

import com.ajavac.dto.FTPInfo;
import com.ajavac.dto.UserInfo;
import com.ajavac.ftp.MyFtpServer;
import org.apache.ftpserver.ftplet.FtpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Created by wyp0596 on 05/05/2017.
 */
@RestController
public class FtpAPI {

    @Autowired
    private MyFtpServer myFtpServer;

    public FTPInfo info() throws FtpException {
        return myFtpServer.getFTPInfo();
    }

    public FTPInfo setMaxUploadRate(@RequestBody FTPInfo ftpInfo) throws FtpException, IOException {
        myFtpServer.setMaxUploadRate(ftpInfo.getMaxUploadRate());
        return myFtpServer.getFTPInfo();
    }

    public FTPInfo setMaxDownloadRate(@RequestBody FTPInfo ftpInfo) throws FtpException, IOException {
        myFtpServer.setMaxDownloadRate(ftpInfo.getMaxDownloadRate());
        return myFtpServer.getFTPInfo();
    }

    public FTPInfo setHomeDir(@RequestBody FTPInfo ftpInfo) throws FtpException, IOException {
        myFtpServer.setHomeDir(ftpInfo.getHomeDir());
        return myFtpServer.getFTPInfo();
    }

    public FTPInfo setPassword(@RequestBody UserInfo userInfo) throws FtpException {
        myFtpServer.setPassword(userInfo);
        return myFtpServer.getFTPInfo();
    }
}
