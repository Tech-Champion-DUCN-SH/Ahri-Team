package com.ericsson.coder

/*
 * this class transit code(10) to code(62)
 */
object Transition {
	/**
	 * this function converts 10 based value to 62 based
	 */
	def convertTo62(id:Int):String = {
	  var ret = new StringBuilder();
	  var temp = id;
	  while(temp > 0){
	    val left = temp % 62;
	    ret.append(convertTo62Char(left));    
	    temp = temp / 62;
	  }
	  
	  //for(j <- ret.length to 5){
	  //  ret.append("0");
	  //}
	  return ret.reverse.toString();
	}
	
	def convertTo10(value:String) : Int = {
	  var pos = value.length() - 1
	  var pow = 0
	  var ret:Double = 0
	  while(pos >=0){
	    ret += math.pow(62, pow) * convertTo10Char(value.charAt(pos));
	    pos -= 1;
	    pow += 1;
	  }
	  
	  return ret.intValue();
	}
	
	/**
	 * this function converts 0-61 to 0-9A-Za-z
	 */
	def convertTo62Char(value:Int):Char = {
	  var rel:Char = 0;
	  if( value <= 9  && value >= 0) {
	    return (value + '0').toChar;
	  } else if( value >= 10 && value <= 35)	{
	    return (value - 10 + 'A').toChar;
	  } else if( value >= 36 && value <= 61){
	    return (value - 36 + 'a').toChar;
	  }
	  
	  return rel;
	}
	
	/**
	 * this function converts 0-9A-Za-z to 0-61
	 */
	def convertTo10Char(value:Char):Int = {
	  val asc = value.toInt;
	  if( value <= '9'  && value >= '0') {
	    return asc - '0';
	  } else if( value >= 'A' && value <= 'Z')	{
	    return asc - 'A' + 10
	  } else if( value >= 'a' && value <= 'z'){
	    return asc - 'a' + 36
	  }
	  
	  return -1;
	}
}