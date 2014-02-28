package com.ericsson.nio

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.io.IOException
import java.nio.channels.SocketChannel
import java.util.ConcurrentModificationException
import com.ericsson.nio.ioHandler.ServerSocketAcceptor
import com.ericsson.nio.ioHandler.SocketAttachment
import java.nio.ByteBuffer
import com.ericsson.nio.ioHandler.PublicWriter

object NIOServer extends Runnable {
    var serverPort = 8081;
    //val THREAD_COUNT = 4
    
	val executor:ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 2);
	val sel:Selector = Selector.open
	
	val ssc:ServerSocketChannel = ServerSocketChannel.open

	/**
	 * open port listen to public
	 */
	def startListen(portNumber:Int) {
      ssc.socket().bind(new InetSocketAddress(portNumber));
      ssc.configureBlocking(false);
      ssc.register(sel, SelectionKey.OP_ACCEPT,new ServerSocketAcceptor(ssc)); //open port to public,accepting requests
    }
    
    def run	{
        //println("Staring NIO thread...");
        while(sel.isOpen()){
          try{
              val retKeys = sel.select(100);
	          if(retKeys > 0)	{
	            val selectedKeys = sel.selectedKeys();
	            if(selectedKeys.isEmpty() == false)	{
	            	val it:java.util.Iterator[SelectionKey] = selectedKeys.iterator();
		            while(it.hasNext()){
			           val sk:SelectionKey = it.next();
			           it.remove();
			           try{
			              if(sk.isAcceptable()){
			                val r:Runnable = sk.attachment().asInstanceOf[Runnable];
			                if(r != null) r.run();
			                else println("acceptor is null!");
			              } else if(sk.isReadable())	{
			                val r:Runnable = sk.attachment().asInstanceOf[SocketAttachment].reader ;
			                if(r != null) r.run();
			                else println("reader is null!");
			              } else if(sk.isWritable())	{
			                val r:Runnable = sk.attachment().asInstanceOf[SocketAttachment].writer ;
			                if(r != null) r.run();
			                else println("writer is null!"); 
			              } else if(sk.isConnectable())	{
			                val r:Runnable = sk.attachment().asInstanceOf[SocketAttachment].connecter;
			                if(r != null) r.run();
			              }
		              } catch {
		                case e:Exception => {e.printStackTrace();try {sk.cancel(); sk.channel().close();} catch{case e:Exception=>{e.printStackTrace()}}}
		              }
		            }
	             }
	          }
          } catch {
            case e:ConcurrentModificationException => { e.printStackTrace();}
          }
        }
    }
}