package com.ericsson.http

import java.util.concurrent.ConcurrentHashMap
import scala.io.BufferedSource
import java.io.BufferedInputStream
import scala.collection.mutable.ArrayBuffer
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.io.File;

object ResourceCenter {
	val resourceCache:ConcurrentHashMap[String,Array[Byte]] = new ConcurrentHashMap[String,Array[Byte]];
	
	def getResource(loc:String):Array[Byte] = {
	  //System.out.println(System.getProperty("user.dir"));
	  if(resourceCache.containsKey(loc))	{
	    return resourceCache.get(loc)
	  } else	{
	    //load resource
	    try{
	      //println(System.getProperty("user.dir")+"/"+loc);
	      val file = new File(System.getProperty("user.dir")+"/"+loc);
	      val length = file.length().toInt;
	      //println("trying to read " + System.getProperty("user.dir")+"/"+loc + " length is " + file.length());
	      
	      val in = new BufferedInputStream(new FileInputStream(System.getProperty("user.dir")+"/"+loc)); 
	      val data = new Array[Byte](length);
	      var pos = 0;
	      var len = 0;
	      while( {len = in.read(data, pos, length - pos);len } > 0){
	        pos += len;      
	      }
	      resourceCache.put(loc, data);
	      //println("Loading " + loc + "length=" + data.length)
	      return data;
	    }catch 	{
	      case e:Exception => return null;
	    }
	    
	  }
	}
}