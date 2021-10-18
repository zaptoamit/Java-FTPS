package com.ajavac.ftp;

import com.ajavac.dto.FTPInfo;
import com.ajavac.dto.UserInfo;
import com.ajavac.util.Properties;
import com.ajavac.util.PropertiesHelper;
import org.apache.ftpserver.*;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfiguration;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ftpserver
 * Created by wyp0596 on 17/04/2017.
 */
@Component
public class MyFtpServer {

    private static final Logger logger = LoggerFactory.getLogger(MyFtpServer.class);

    private FtpServer ftpServer;
    private UserManager um;

    private static final String CONFIG_FILE_NAME = "application.properties";
    private static final String USERS_FILE_NAME = "users.properties";
    private static final int MAX_IDLE_TIME = 300;

    @Value("${server.host}")
    private String host;
    @Value("${ftp.port}")
    private int port;
    @Value("${ftp.passive-ports}")
    private String passivePorts;
    @Value("${ftp.max-login}")
    private Integer maxLogin;
    @Value("${ftp.max-threads}")
    private Integer maxThreads;
    @Value("${ftp.username}")
    private String username;
    @Value("${ftp.password}")
    private String password;
    @Value("${ftp.home-dir}")
    private String homeDir;


    @PostConstruct
    private void start() {
        //Check if the directory exists, create the directory if it does not exist
        mkHomeDir(homeDir);
        //Create a configuration file
        try {
            createConfigFile();
        } catch (IOException e) {
            logger.warn("Abnormal configuration file creation", e);
        }

        //ListenerFactory listenerFactory = new ListenerFactory();
        //Configure FTP port


        SslConfigurationFactory ssl = new SslConfigurationFactory();
        ssl.setKeystoreFile( new File( "serverkeystore.p12" ) );
        ssl.setKeystorePassword( "password" );
        // set the SSL configuration for the listener
        //listenerFactory.setSslConfiguration( ssl.createSslConfiguration() );
        //listenerFactory.setImplicitSsl( true );
        ListenerFactory listenerFactory = configureSSL();
        listenerFactory.setPort(port);

        FtpServerFactory serverFactory = new FtpServerFactory();

        // FTP service connection configuration
        ConnectionConfigFactory connectionConfigFactory = new ConnectionConfigFactory();
        connectionConfigFactory.setAnonymousLoginEnabled(false);
        connectionConfigFactory.setMaxLogins(maxLogin);
        connectionConfigFactory.setMaxThreads(maxThreads);
        serverFactory.setConnectionConfig(connectionConfigFactory.createConnectionConfig());


        // Passive mode configuration (on demand)
        if (!Objects.equals(passivePorts, "")) {
            DataConnectionConfigurationFactory dataConnectionConfFactory = new DataConnectionConfigurationFactory();
            logger.info("Perform passive mode configuration, passive port number range:{}", passivePorts);
            dataConnectionConfFactory.setPassivePorts(passivePorts);
            if (!(Objects.equals(host, "localhost") || Objects.equals(host, "127.0.0.1"))) {
                logger.info("Perform passive mode configuration, local address: ()", host);
                dataConnectionConfFactory.setPassiveExternalAddress(host);
            }
            listenerFactory.setDataConnectionConfiguration(
                    dataConnectionConfFactory.createDataConnectionConfiguration());
        }

        //Replace the default listener
        serverFactory.addListener("default", listenerFactory.createListener());


        //Set up user control center
        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        userManagerFactory.setFile(new File(USERS_FILE_NAME));
        userManagerFactory.setAdminName(username);
        um = userManagerFactory.createUserManager();
        try {
            initUser();
        } catch (FtpException e) {
            logger.warn("init user fail:", e);
            return;
        }
        serverFactory.setUserManager(um);

        //Create and start FTP service
        ftpServer = serverFactory.createServer();
        try {
            ftpServer.start();
        } catch (FtpException e) {
            logger.warn("FTP startup abnormal", e);
            throw new RuntimeException(e);
        }
        logger.info("ftp started successfully, port number:" + port);
    }

    private ListenerFactory configureSSL() {
        ListenerFactory listener = new ListenerFactory();
        listener.setServerAddress("127.0.0.1");
        listener.setPort(port);

        SslConfigurationFactory ssl = new SslConfigurationFactory();
        ssl.setKeystoreFile(new File("serverkeystore.p12"));
        ssl.setKeyPassword("password");
        ssl.setKeystorePassword("password");
        ssl.setTruststoreFile(new File("server.truststore"));
        ssl.setTruststorePassword("password");
        ssl.setClientAuthentication("NEED");

        SslConfiguration sslConfig = ssl.createSslConfiguration();

        listener.setSslConfiguration(sslConfig);
        listener.setImplicitSsl(true);
        DataConnectionConfigurationFactory dataConfigFactory = new DataConnectionConfigurationFactory();
        dataConfigFactory.setImplicitSsl(true);

        listener.setDataConnectionConfiguration(dataConfigFactory.createDataConnectionConfiguration());

        return listener;
    }

    @PreDestroy
    private void stop() {
        if (ftpServer != null) {
            ftpServer.stop();
        }
    }

    private void initUser() throws FtpException {
        boolean exist = um.doesExist(username);
        // need to init user
        if (!exist) {
            List<Authority> authorities = new ArrayList<>();
            authorities.add(new WritePermission());
            authorities.add(new ConcurrentLoginPermission(0, 0));
            BaseUser user = new BaseUser();
//            user.setName(username);
//            user.setPassword(password);
            user.setHomeDirectory(homeDir);
            user.setMaxIdleTime(MAX_IDLE_TIME);
            user.setAuthorities(authorities);
            um.save(user);
        }
    }

    /**
     * change Password
     *
     * @param userInfo User Info
     * @throws FtpException                  FTP exception
     * @throws AuthenticationFailedException Verify user exception
     */
    public void setPassword(UserInfo userInfo) throws FtpException {
        String username = um.getAdminName();
        User savedUser = um.authenticate(new UsernamePasswordAuthentication(username, userInfo.getOldPassword()));
        BaseUser baseUser = new BaseUser(savedUser);
        baseUser.setPassword(userInfo.getPassword());
        um.save(baseUser);
    }

    /**
     * Modify the home directory
     *
     * @param homeDir Home directory, can be a relative directory
     * @throws FtpException FTP exception
     */
    public void setHomeDir(String homeDir) throws FtpException, IOException {
        User userInfo = um.getUserByName(um.getAdminName());
        BaseUser baseUser = new BaseUser(userInfo);
        mkHomeDir(homeDir);
        baseUser.setHomeDirectory(homeDir);
        um.save(baseUser);
        //Save configuration
        Properties ftpProperties = PropertiesHelper.getProperties(CONFIG_FILE_NAME);
        if (!homeDir.endsWith("/")) {
            homeDir += "/";
        }
        ftpProperties.setProperty("ftp.home-dir", homeDir);
        PropertiesHelper.saveProperties(ftpProperties, CONFIG_FILE_NAME);
    }

    /**
     * Modify the maximum download speed
     *
     * @param maxDownloadRate Maximum download speed, unit KB
     * @throws FtpException FTP exception
     */
    public void setMaxDownloadRate(int maxDownloadRate) throws FtpException {
        int maxUploadRate = getFTPInfo().getMaxUploadRate();
        saveTransferRateInfo(maxUploadRate * 1024, maxDownloadRate * 1024);
    }

    /**
     * Modify the maximum upload speed
     *
     * @param maxUploadRate Maximum upload speed, unit KB
     * @throws FtpException FTP exception
     */
    public void setMaxUploadRate(int maxUploadRate) throws FtpException {
        int maxDownloadRate = getFTPInfo().getMaxDownloadRate();
        saveTransferRateInfo(maxUploadRate * 1024, maxDownloadRate * 1024);
    }

    /**
     *
     * Save transfer rate limit information
     *
     * @param maxUploadRate   Maximum upload speed, unit B
     * @param maxDownloadRate Maximum download speed, unit B
     * @throws FtpException FTP exception
     */
    private void saveTransferRateInfo(int maxUploadRate, int maxDownloadRate) throws FtpException {
        User userInfo = um.getUserByName(um.getAdminName());
        BaseUser baseUser = new BaseUser(userInfo);
        List<Authority> authorities = new ArrayList<>();
        authorities.add(new WritePermission());
        authorities.add(new TransferRatePermission(maxDownloadRate, maxUploadRate));
        baseUser.setAuthorities(authorities);
        um.save(baseUser);
    }

    /**
     * Get FTP information
     *
     * @return FTP information
     * @throws FtpException FTP exception
     */
    public FTPInfo getFTPInfo() throws FtpException {
        User userInfo = um.getUserByName(um.getAdminName());
        TransferRateRequest transferRateRequest = (TransferRateRequest) userInfo
                .authorize(new TransferRateRequest());
        File homeDir = Paths.get(userInfo.getHomeDirectory()).toFile();
        long totalSpace = homeDir.getTotalSpace();
        long usedSpace = totalSpace - homeDir.getUsableSpace();

        return new FTPInfo(host, port, homeDir.getAbsolutePath(),
                transferRateRequest.getMaxDownloadRate() / 1024,
                transferRateRequest.getMaxUploadRate() / 1024,
                usedSpace, totalSpace);
    }

    private void mkHomeDir(String homeDir) {
        try {
            Files.createDirectories(Paths.get(homeDir, "temp"));
        } catch (IOException e) {
            logger.warn("Failed to create directory");
            throw new UncheckedIOException(e);
        }
    }

    private void createConfigFile() throws IOException {
        File configFile = new File(CONFIG_FILE_NAME);
        if (!configFile.exists()) {
            boolean result = configFile.createNewFile();
            if (!result) {
                logger.warn("Failed to create configuration file");
            }
        }
        File usersFile = new File(USERS_FILE_NAME);
        if (!usersFile.exists()) {
            boolean result = usersFile.createNewFile();
            if (!result) {
                logger.warn("Failed to create configuration file");
            }
        }
    }
}
