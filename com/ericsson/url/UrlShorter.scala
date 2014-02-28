package com.ericsson.url
import java.util.concurrent.ConcurrentHashMap
import com.ericsson.coder.Transition._
import scala.io.Source
import java.io.PrintWriter
import java.io.FileWriter

object UrlShorter {
	var index:Int = 0;
	var database:ConcurrentHashMap[String,String] = new ConcurrentHashMap[String,String]();
	
	def putUrl(url:String,index:Int):String = {
	  var shortUrl =  convertTo62(index);  
	  database.put(shortUrl, url);
	  //DataSerializer.put(shortUrl, url);//no persistency is needed
	  return shortUrl;
	}
	
	def getIndex():Int = synchronized	{
	  index += 1;
	  return index;
	}
	
	def getUrl(shortUrl:String):String = {
	  if(database.containsKey(shortUrl.trim()))	{
	    return database.get(shortUrl.trim());
	  } else	{
	    return null;
	  }
	}
	
	/**
	 * read data files
	 */
	def init()	{
	  try	{
		  val source = Source.fromFile("database.txt","UTF-8");
		  val iterator = source.getLines
		  while(iterator.hasNext)	{
			val line = iterator.next;
			val values = line.split("`");
			if(values.length == 2){
				database.put(values(0), values(1));
			}
			index = database.keySet().size() +1;
			//println(index);
		}
		source.close;
	  } catch {
	    case e:Exception => e.printStackTrace();
	  }
	  new Thread(DataSerializer).start();
	  
	}
	
}