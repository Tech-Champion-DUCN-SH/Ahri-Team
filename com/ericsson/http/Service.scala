package com.ericsson.http
import com.ericsson.nio.NIOServer
import com.ericsson.nio.ClusterWatchDog
import com.ericsson.nio.NIOAcceptor

import com.ericsson.nio.ioHandler.AttachmentPool

object Service {
  def main(args: Array[String]): Unit = {
        AttachmentPool;
        NIOServer.serverPort = 80; //by default,
        //com.ericsson.url.UrlShorter.init; //data persistency is not needed
        new Thread(NIOServer).start();
        //new Thread(NIOAcceptor).start();
        new Thread(ClusterWatchDog).start();
        NIOServer.startListen(NIOServer.serverPort);
        
    }
    
  
}