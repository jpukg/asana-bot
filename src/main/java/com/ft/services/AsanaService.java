package com.ft.services;

import com.ft.asanaapi.AsanaClient;
import com.ft.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(Config.class)
public class AsanaService {

    @Autowired private AsanaClient picturesAsanaClient;
    @Autowired private AsanaClient graphicsAsanaClient;

    @Autowired private Config config;

    public void addGraphicsProjectToGraphicsBotAssignedTasks(){
        graphicsAsanaClient.addProjectToCurrentlyAssignedIncompleteTasks(config.getGraphicsId());
    }

    public void addPicturesProjectToPicturesBotAssignedTasks(){
        picturesAsanaClient.addProjectToCurrentlyAssignedIncompleteTasks(config.getPicturesId());
    }
}
