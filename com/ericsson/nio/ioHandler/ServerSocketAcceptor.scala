package com.ericsson.nio.ioHandler

import java.nio.channels.SocketChannel
import java.nio.channels.ServerSocketChannel
import java.nio.channels.Selector
import java.nio.channels.SelectionKey
import java.io.IOException
import java.util.concurrent.ExecutorService
import com.ericsson.nio.NIOServer
import com.ericsson.nio.ClusterWatchDog


class ServerSocketAcceptor(ssc:ServerSocketChannel) extends Runnable {
	def run	{
	  try	{
	    val sc:SocketChannel = ssc.accept();
	    
	    //overload protected
	    if(ClusterWatchDog.cpuOverloaded)	{
	      sc.close();
	    }else if(sc != null)	{
	      sc.configureBlocking(false);
	      sc.socket().setTcpNoDelay(true);
	      sc.socket().setSoLinger(false, -1);
	      val sk:SelectionKey = sc.register(NIOServer.sel, SelectionKey.OP_READ);
	      val handlers =	AttachmentPool.getAttach(sk);
	      
	      sk.attach(handlers);
	      NIOServer.sel.wakeup();
	    }
	  } catch 	{
	    case e:IOException => e.printStackTrace();
	  }
	}
}