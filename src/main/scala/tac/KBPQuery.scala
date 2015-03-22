package tac

import scala.xml.XML
import KBPQueryEntityType._

case class KBPQuery (val id: String, val name: String, val doc: String,
    val begOffset: Int, val endOffset: Int, val entityType: KBPQueryEntityType,
    val slotsToFill: Set[Slot] ){
  
  def aliases():List[String] = name :: List[String]()
  
  override def toString():String = id + "\t" + name
  
}

object KBPQuery {

  import KBPQueryEntityType._

  
  private def parseSingleKBPQueryFromXML(queryXML: scala.xml.Node): Option[KBPQuery] = {

    try{
	    val idText = queryXML.attribute("id") match 
	    		{case Some(id) if id.length ==1 => id(0).text
	    		 case None => throw new IllegalArgumentException("no id value for query in xml doc")
	    		}
	    val nameText = queryXML.\\("name").text
	    val docIDText = queryXML.\\("docid").text
	    val begText = queryXML.\\("beg").text
	    val begInt = begText.toInt
	    val endText = queryXML.\\("end").text
	    val endInt = endText.toInt
	    val entityTypeText = queryXML.\\("enttype").text
	    val entityType = entityTypeText match {
	      case "ORG" => ORG
	      case "PER" => PER
	      case _ => throw new IllegalArgumentException("improper 'enttype' value in xml doc")
	    }
	//val nodeIDText = queryXML.\\("nodeid").text.trim()
    //val nodeId = if (nodeIDText.isEmpty || nodeIDText.startsWith("NIL")) None else Some(nodeIDText)
    //val ignoreText = queryXML.\\("ignore").text
    /*val ignoreSlots = {
       val ignoreNames = ignoreText.split(" ").toSet
       Slot.getSlotTypesList(entityType).filter(slot => ignoreNames.contains(slot.name))
    }*/
	    
	//find slotsToFill by taking the difference between the global slots set
    // and the set specified in the xml doc
    /*val slotsToFill = entityType match{
       case ORG => {
          Slot.orgSlots &~ ignoreSlots
       }
       case PER => {
          Slot.personSlots &~ ignoreSlots
       }
    }*/
	
    val slotsToFill = entityType match{
       case ORG => {
          Slot.orgSlots
       }
       case PER => {
          Slot.personSlots
       }
    } 

	//println("SlotsToFill Head: " + slotsToFill.head.name)
	//println("SlotsToFill Tail: " + slotsToFill.tail.head.name)
	
	    new Some(KBPQuery(idText,nameText,docIDText,begInt,endInt,entityType,slotsToFill))
    }
    
    catch {
      case e: Exception => {
        println(e.getMessage())
        return None
        
      }
    }      
  }
  

  def parseKBPQueries(pathToFile: String): List[KBPQuery] = {
    
     val xml = XML.loadFile(pathToFile)
     val queryXMLSeq = xml.\("query")
     
     val kbpQueryList = for( qXML <- queryXMLSeq) yield parseSingleKBPQueryFromXML(qXML)
     
     kbpQueryList.toList.flatten
  }
  
}