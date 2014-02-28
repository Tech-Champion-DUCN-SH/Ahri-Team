package com.ericsson.http

import java.nio.channels.SelectionKey
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import com.ericsson.nio.ioHandler.SocketAttachment
import java.nio.ByteBuffer
import com.ericsson.nio.ClusterWatchDog
import java.nio.channels.SocketChannel
import java.net.URL
import java.net.URLDecoder
import java.net.InetSocketAddress

class MonoHandler extends Runnable {
    var headersBytes:Array[Byte] = null;
    var sk:SelectionKey = null;
    val sb:StringBuilder = new StringBuilder;
    val output = new ByteArrayOutputStream(BUFFER_SIZE); //optimized for pool , no new when handling every request
    //for performance reason,all the response are almost the same, so  hard-code all the response as much as possible,although I hate it
    val BUFFER_SIZE = 1024;
    val jsonNotFound = "{\r\n    \"error\": true,\r\n    \"code\": 404,\r\n    \"message\": \"Not found\"\r\n}";
    
    //make up the 404 response
    sb.append("HTTP/1.1 404 Not Found\r\nConnection: close\r\nContent-Length: ");
    sb.append(jsonNotFound.getBytes().length);
    sb.append("\r\n\r\n");
    sb.append(jsonNotFound);
    val notFoundRes = sb.toString.getBytes();
    
    val okRes = "HTTP/1.1 200 OK\r\nConnection: close\r\nContent-Type: application/json\r\n".getBytes();

	def run	{
	  
	  val br:BufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(headersBytes)));
	  
	  var line:String = br.readLine();
      val httpOperations = line.split(" ");
      
      //now everything is ready, call handler
      output.reset();

      if(httpOperations(1).startsWith("/urlshortener/url?shortUrl=")){
        val url = httpOperations(1).substring(27, httpOperations(1).length());
        resolve(output,url);
      } else if(httpOperations(1).startsWith("/urlshortener/url?longUrl="))	{
        val url = httpOperations(1).substring(26, httpOperations(1).length());
        registerUrl(output,url);
      } else	{
        sendNotFound();
        sk.attachment().asInstanceOf[SocketAttachment].totalTransaction += 1;
      }
	}
	
	def registerUrl(output:ByteArrayOutputStream,longUrl:String)	{
	  if(ClusterWatchDog.memOverloaded){
	    sendNotFound();
	  }
	  val index = com.ericsson.url.UrlShorter.getIndex;
	  val decodedUrl = URLDecoder.decode(longUrl);
	  val shortUrl = com.ericsson.url.UrlShorter.putUrl(decodedUrl,index);
	  sendOK(output,decodedUrl, shortUrl,false);
	  
	  sk.attachment().asInstanceOf[SocketAttachment].totalRecords += 1;
	  sk.attachment().asInstanceOf[SocketAttachment].totalTransaction += 1;
	}
	
	def resolve(output:ByteArrayOutputStream,reqUrl:String)	{
	  val decodedUrl = URLDecoder.decode(reqUrl);
	  val pos = decodedUrl.lastIndexOf("/");
	  val shortUrl = decodedUrl.substring(pos+1, decodedUrl.length());
	  
	  if(shortUrl.length() > 6)
	    sendNotFound();
	  else	{
	    var longUrl = com.ericsson.url.UrlShorter.getUrl(shortUrl);
			if(longUrl == null) 	{//shortUrl does not exist
				sendNotFound();
			} else	{
				sendOK(output,longUrl, shortUrl,true);
			}
	  }
	  
	  sk.attachment().asInstanceOf[SocketAttachment].totalTransaction += 1;
	}
	
	def appendJson(buffer:StringBuilder,prop:String,value:String, last:Boolean =false)	{
	  buffer.append("    \"");
	  buffer.append(prop);
	  buffer.append("\": \"");
	  buffer.append(value);
	  if(last)
		buffer.append("\"\r\n");
	  else
		buffer.append("\",\r\n");
	}
	
	def sendOK(output:ByteArrayOutputStream, long:String, short:String, kindLong:Boolean){
		output.write(okRes);
		
		//make up json response
		sb.clear;
		sb.append("{\r\n");
		appendJson(sb,"kind",{if(kindLong)"expand" else "shorten"});
		appendJson(sb,"shortUrl","http:/" + sk.channel().asInstanceOf[SocketChannel].getLocalAddress().asInstanceOf[InetSocketAddress].getAddress().toString() + "/" + short);
		//println(sk.channel().asInstanceOf[SocketChannel].getLocalAddress().toString());
		appendJson(sb,"longUrl",long,true);
		sb.append("}");
		val contentBytes = sb.toString.getBytes();
		
		sb.clear;
		sb.append("Content-Length: ");
		sb.append(contentBytes.length.toString);
		sb.append("\r\n\r\n");
		output.write(sb.toString.getBytes());
		output.write(contentBytes);
		sk.attachment().asInstanceOf[SocketAttachment].writer.sendData(ByteBuffer.wrap(output.toByteArray()));
	}
	
	def sendNotFound(){
	  sk.attachment().asInstanceOf[SocketAttachment].writer.sendData(ByteBuffer.wrap(notFoundRes));
	}
	
}