/*
 * $Id: IDOTableCreator.java,v 1.62 2008/11/05 16:39:41 laddi Exp $
 * 
 * Copyright (C) 2001-2006 Idega Software hf. All Rights Reserved.
 * 
 * This software is the proprietary information of Idega hf. Use is subject to
 * license terms.
 */
package com.idega.data;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.TransactionManager;

import com.idega.idegaweb.IWMainApplication;
import com.idega.io.MemoryFileBuffer;
import com.idega.io.MemoryInputStream;
import com.idega.io.MemoryOutputStream;
import com.idega.transaction.IdegaTransactionManager;
import com.idega.user.data.GroupBMPBean;
import com.idega.util.FileUtil;
import com.idega.util.ThreadContext;
import com.idega.util.Timer;
import com.idega.util.database.ConnectionBroker;
import com.idega.util.database.PoolManager;
import com.idega.util.logging.LoggingHelper;


/**
 * <p>
 * Class that handles the creation and generation of the (DDL) commands for creating and
 * updating database tables for IDO Entity beans.
 * </p>
 * Last modified: $Date: 2008/11/05 16:39:41 $ by $Author: laddi $
 * 
 * @author <a href="mailto:tryggvil@idega.com">Tryggvi Larusson</a>
 * @version $Revision: 1.62 $
 */
public class IDOTableCreator {

  private static String recordCreationKey="datastoreinterface_entity_record_creation";
  private DatastoreInterface _dsi;
  private List _entityWithStartData = new Stack();
  private boolean useCopyLog = false;
  private static String sourceDatasource = "sourceDB";
  
  protected IDOTableCreator(DatastoreInterface dsi){
    this._dsi=dsi;
  }

  protected void executeQuery(GenericEntity entity,String SQLCode)throws Exception{
    this._dsi.executeQuery(entity,SQLCode);
  }

  protected int executeUpdate(GenericEntity entity,String SQLCode)throws Exception{
    return this._dsi.executeUpdate(entity,SQLCode);
  }

  /**
   * Returns a List whichs elements are the Classes that the EntityAutoCreator is still creating
   */
  protected List getCreationList(){
      List alreadyInCreation=(List)ThreadContext.getInstance().getAttribute(recordCreationKey);
      if(alreadyInCreation==null){
        alreadyInCreation=new Vector();
        ThreadContext.getInstance().setAttribute(recordCreationKey,alreadyInCreation);
      }
      return alreadyInCreation;
  }

  protected void registerEndOfCreatingEntity(GenericEntity entity){
      List alreadyInCreation=(List)ThreadContext.getInstance().getAttribute(recordCreationKey);
      if(alreadyInCreation!=null){
        alreadyInCreation.remove(entity.getClass());
        if(alreadyInCreation.isEmpty()){
          ThreadContext.getInstance().removeAttribute(recordCreationKey);
        }
      }
  }

  protected void registerStartOfCreatingEntity(GenericEntity entity){
      //Code block to prevent circular recursiveness
      //i.e. that it infinately recurses through the same entity when it is circularly referenced
      List alreadyInCreation=this.getCreationList();
      if(!hasAlreadyStartedCreatingEntity(entity)){
        alreadyInCreation.add(entity.getClass());
//        try {
//			createRefrencedTables(entity);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
      }
  }

  protected boolean hasAlreadyStartedCreatingEntity(GenericEntity entity){
    List alreadyInCreation=this.getCreationList();
    return alreadyInCreation.contains(entity.getClass());
  }

  protected boolean startEntityCreationTransaction(GenericEntity entity,boolean isPermittedToCommit){
    TransactionManager trans=null;
    boolean canCommit=isPermittedToCommit;
    try{
      if(this._dsi.useTransactionsInEntityCreation){
        trans = com.idega.transaction.IdegaTransactionManager.getInstance(entity.getClass());
        if(!((IdegaTransactionManager)trans).hasCurrentThreadBoundTransaction()){
          this._dsi.executeBeforeCreateEntityRecord(entity);
          ((IdegaTransactionManager)trans).setEntity(entity);
          trans.begin();
          canCommit=true;
        }
        else{
          canCommit=false;
        }
      }
      registerStartOfCreatingEntity(entity);
    }
    catch(Exception e){
      e.printStackTrace();
    }
    return canCommit;
  }


  protected void endEntityCreationTransaction(GenericEntity entity,boolean isPermittedToCommit,boolean transactionSuccessful){
      boolean canCommit = isPermittedToCommit;
      try{
        TransactionManager trans = com.idega.transaction.IdegaTransactionManager.getInstance();
        if(this._dsi.useTransactionsInEntityCreation){
          if(canCommit){
            if(transactionSuccessful){
              //System.out.println("\t\t\tCommitting!!!!");
              trans.commit();
              registerEndOfCreatingEntity(entity);
            }
            else{
              //System.out.println("\t\t\tRollbacking!!!!");
              trans.rollback();
              registerEndOfCreatingEntity(entity);
            }
            ThreadContext.getInstance().removeAttribute(recordCreationKey);
            this._dsi.executeAfterCreateEntityRecord(entity);
            //ThreadContext.getInstance().releaseThread(Thread.currentThread());
          }
          else{
            if(transactionSuccessful){
              //System.out.println("\t\t\tNot permitted to commit!!");
            }
            else{
              trans.setRollbackOnly();
              //System.out.println("\t\t\tNot permitted to Rollback!!");
            }
          }
        }
      }
      catch(Exception e){
        e.printStackTrace();
      }
  }
  
  protected boolean doesViewExist(GenericView entityView){
  	boolean returner =true;
  	String viewName =entityView.getViewName();
  	Timer timer = new Timer();
  	timer.start();
  	try {
		if(!this._dsi.doesViewExist(entityView.getDatasource(),viewName)) {
			throw new Exception("View "+viewName+"does not exists");
		}
	}
	catch (Exception e) {
		returner =false;
	}
    timer.stop();
    debug("doesViewExist() check took "+(timer.getTime())+" milliseconds"+"  ("+viewName+")");
  	return returner;
  }


  protected boolean doesTableExist(GenericEntity entity,String tableName){
    boolean theReturner=true;
    try{
    	long start = System.currentTimeMillis();
    	/**
    	 * @todo: Change to doTableCheckDatabaseMetadata()
    	 **/
    	//doTableCheckSelectStar(entity,tableName);
    	doTableCheckDatastoreInterface(entity,tableName);
        //doTableCheckDatabaseMetadata(entity,tableName);
        long end = System.currentTimeMillis();
		debug("doesTableExist() check took "+((end-start))+" milliseconds"+"  ("+tableName+")");
      
    }
    catch(Exception se){
      //String message = se.getMessage();
      //if(message.toLowerCase().indexOf("table")!=-1){
        theReturner=false;
        //System.out.println("Table: "+tableName+" does not exist, exception:"+se.getClass().getName());
      //}
      //else{
        //se.printStackTrace();
      //}
    }
    return theReturner;
  }
  
  
  private void doTableCheckDatastoreInterface(GenericEntity entity,String tableName)throws Exception{
	  if(!this._dsi.doesTableExist(entity.getDatasource(),tableName)) {
			throw new Exception("Table "+tableName+"does not exists");
		}
  }


	
	/**
	 * Creates an entity record (view) that represents the view entity in the datastore
	 */
	public void createEntityView(GenericView entityView){
		if(!doesViewExist(entityView)){
			boolean canCommit=false;
			debug("Creating "+entityView.getClass().getName()+" - view: "+entityView.getTableName());
			try {
				canCommit = this.startEntityCreationTransaction(entityView,canCommit);
				//Create the records of all referenced entities (which this entity has dependent relationships on)
				if(!this.hasAlreadyStartedCreatingEntity(entityView)){
					createRefrencedTables(entityView);
				}
				createView(entityView);
				//commit
				this.endEntityCreationTransaction(entityView,canCommit,true);
			}
			catch (Exception e) {
				log(e);
				// rollback
				this.endEntityCreationTransaction(entityView,canCommit,false);
				e.printStackTrace();
			}
		} 
		else{
			debug("Synchronizing  "+entityView.getClass().getName()+" - viewname: "+entityView.getViewName());
		}
	}

  /**
   * Creates an entity record (table) that represents the entity in the datastore
   */
  public void createEntityRecord(GenericEntity entity)throws Exception{
//		 entity = (GenericEntity) com.idega.data.GenericEntity.getStaticInstanceIDO(ICFile.class);

    if(!doesTableExist(entity,entity.getTableName())){
		//if(this.isDebugActive())
    	debug("Creating "+entity.getClass().getName()+" - tablename: "+entity.getTableName());
    	
      boolean canCommit=false;
      canCommit = this.startEntityCreationTransaction(entity,canCommit);
      
      try{

        //Create the records of all referenced entities (which this entity has dependent relationships on)
        if(!this.hasAlreadyStartedCreatingEntity(entity)){
          createRefrencedTables(entity);
        }



        //Check again if table exists because it could be created through createRefrencedTables(entity)
        if(!this.doesTableExist(entity,entity.getTableName())){
          createTable(entity);
          createTrigger(entity);
          try{
            createForeignKeys(entity);
          }
          catch(Exception e){
           e.printStackTrace();
            //System.err.println("Exception in creating Foreign Keys for: "+entity.getClass().getName());
            //System.err.println("  Error was: "+e.getMessage());
          }
          
          // CHECKING IF COPYING
          if (PoolManager.getInstance().hasDatasource(sourceDatasource)) {
        	  useCopyLog = true;
        	  IDOHome sourceHome = IDOLookup.getHome(entity.getInterfaceClass(), sourceDatasource);
//        	  IDOHome localHome = IDOLookup.getHome(entity.getInterfaceClass());
    		 GenericEntity ent = (GenericEntity) com.idega.data.GenericEntity.getStaticInstanceIDO(entity.getInterfaceClass(),sourceDatasource);
    		 ent.setDatasource(sourceDatasource, false);
 
    		 System.out.println("Getting data from "+ent.getEntityName());
    		 Class pkClass = ent.getPrimaryKeyClass();
    		 Collection results = null;
    		 Collection ids = null;
    		 if (IDOPrimaryKey.class.isAssignableFrom(pkClass)) {
    			 //for combined primary keys we must fetch one by one, and the order shouldn't matter since no generators exist for them
        		 ids = ent.idoFindPKsBySQL("select * from "+ent.getEntityName());
        		 results = sourceHome.getEntityCollectionForPrimaryKeys(ids);
    		 } else {
    			 //for single primary keys we can fetch in batches!
        		 ids = ent.idoFindPKsBySQL("select * from "+ent.getEntityName()+" order by "+ent.getIDColumnName());
        		 Collection pkList = ent.idoFindByPrimaryKeyCollection(ids,_dsi.getOptimalEJBLoadFetchSize());
        		 results = sourceHome.getEntityCollectionForPrimaryKeys(pkList);
    		 }
    		 //    		 Collection results = sourceHome.getEntityCollectionForPrimaryKeys(ids);
//        	  String[] cols = entity.getColumnNames();
//        	  Iterator i = ids.iterator();
    		 Iterator i = results.iterator();
    		 int counter = 0;
           	  System.out.print("Starting copy ("+ids.size()+")...");
        	  while (i.hasNext()) {
        		  MemoryFileBuffer buffer = null;
        		  BufferedInputStream bis = null;
        		  MemoryOutputStream bos = null;
        		  MemoryInputStream mis = null;
        		  try {
//        			  Object pk = (Object) i.next();
//        			  GenericEntity sourceEnt = (GenericEntity) sourceHome.findByPrimaryKeyIDO(pk);
	        		  GenericEntity sourceEnt = (GenericEntity) i.next();
	        		  if (sourceEnt.hasLobColumn()) {
	        			  bis = new BufferedInputStream(sourceEnt.getInputStreamColumnValue(sourceEnt.getLobColumnName()));
	        			  buffer = new MemoryFileBuffer();
	        			  bos = new MemoryOutputStream(buffer);
	        			  byte[] buff = new byte[10240];
	        			  int bytesRead;
	        			  // Simple read/write loop.
	        			  while (-1 != (bytesRead = bis.read(buff, 0, buff.length))) {
	        				  bos.write(buff, 0, bytesRead);
	        			  }
	        		  }
	        		  sourceEnt.setEntityState(IDOLegacyEntity.STATE_NEW_AND_NOT_IN_SYNCH_WITH_DATASTORE);
	        		  sourceEnt.setDatasource(ConnectionBroker.DEFAULT_POOL, false);
	        		  if (buffer != null) {
	        			  mis = new MemoryInputStream(buffer);
	        			  sourceEnt.setColumn(sourceEnt.getLobColumnName(), mis);
	        		  }
	        		  (sourceEnt).store();
        		  } catch (Exception e) {
        			  logCopyError(entity, e, "data copying");
//        			  e.printStackTrace();
        		  } finally {
        			  if (mis != null) {
        				  mis.close();
        			  }
        			  if (bos != null) {
        				  bos.close();
        			  }
        			  if (bis != null) {
        				  bis.close();
        			  }
        		  }
        		  if (counter % 10000 == 0) {
        			  if (counter != 0) {
        				  System.out.println(":");
            			  System.out.print(counter+" ");
        			  } else {
        				  System.out.println();
        				  System.out.print("      ");
        			  }
        		  } else if (counter % 1000 == 0) {
        			  System.out.print(":");
        		  } else if (counter % 100 == 0) {
        			  System.out.print(".");
        		  }
        		  ++counter;

//        		  GenericEntity targetEnt = (GenericEntity) localHome.createIDO();
//        		  System.out.print("cp : ");
//        		  for (int j = 0; j < cols.length; j++) {
//        			  targetEnt.setColumn(cols[j], sourceEnt.getColumnValue(cols[j]));
//            		  System.out.print(cols[j]+" = "+sourceEnt.getColumnValue(cols[j])+", ");
//        		  }
//        		  System.out.println();
//        		  targetEnt.store();
        	  }
//        	  updateNumberGeneratorValue(entity, (counter+1));
    		  System.out.println(" done : "+counter);
          } else if(entity.getIfInsertStartData()){
              // ELSE NORMAL
	      		this._entityWithStartData.add(entity);
          }
          
          try{
        	  createIndexes(entity);
          }
          catch(Exception e){
        	  e.printStackTrace();
        	  //this can fail but don't kill the transaction then!
          }

          
          createMiddleTables(entity);

        }
        
        
      	this.endEntityCreationTransaction(entity,canCommit,true);

      	try {
      		//boolean notUseTransactions = !_dsi.useTransactionsInEntityCreation;
      		boolean entitiesInList = !this._entityWithStartData.isEmpty();
      		
      		if (entitiesInList) {
	      	//if (canCommit || notUseTransactions && entitiesInList) {
	      		Iterator iter = this._entityWithStartData.iterator();
	      		while (iter.hasNext()) {
	        		GenericEntity tmpEnt = (GenericEntity) iter.next();
	        			try{
	        				debug("[IDOTableCreator] : Inserting start data for entity : "+tmpEnt.getEntityName());
		      				tmpEnt.insertStartData();
	        			}
	        			catch(Exception e){
	        				e.printStackTrace();
	        			}
	      		}
	      		this._entityWithStartData = new Stack();
	      	}
        } catch (Exception e) {
        	System.out.println("===========================================");
        	System.out.println("============"+e.getMessage()+"=============");
        	if (e.getMessage() == null) {
        		e.printStackTrace();
        	}
        	System.out.println("===========================================");
        }


      }
      catch(Exception ex){
		//if(this.isDebugActive()){
        	System.err.println("===");
        	System.err.println("Exception and rollback for: "+entity.getClass().getName());
        	System.err.println("\tMessage: "+ex.getMessage());
        	ex.printStackTrace();
        	System.err.println("===");
		//}
        //ex.printStackTrace();
        /**@todo fix this Tryggvi so that we can use it!**/
        this.endEntityCreationTransaction(entity,canCommit,false);

        //tmp fix
        //this.endEntityCreationTransaction(entity,canCommit,true);
      }
    }
    else{
    		debug("Synchronizing  "+entity.getClass().getName()+" - tablename: "+entity.getTableName());

      boolean canCommit = false;
      canCommit = this.startEntityCreationTransaction(entity,canCommit);
      updateColumns(entity);
      updateIndexes(entity);
      updateTriggers(entity);
      createMiddleTables(entity);
      this.endEntityCreationTransaction(entity,canCommit,true);
    }//End if(!doesTableExist())
    
	
  }

//	private void updateNumberGeneratorValue(GenericEntity entity, int highestValue) {
//		Connection conn = null;
//		Statement stmt = null;
//		ResultSet rs = null;
//		int valueToSet = highestValue;
//		try {
//			conn = entity.getConnection();
//			stmt = conn.createStatement();
//			rs = stmt.executeQuery("select max(" + entity.getIDColumnName() + ") from " + entity.getTableName());
//			rs.next();
//			int i = rs.getInt(1);
//			if (i > valueToSet) {
//				valueToSet = i;
//			}
//			rs.close();
//			stmt.close();
//		}
//		catch (SQLException e) {
//			logCopyError(entity, e, "updating generator");
////			e.printStackTrace();
//		}
//		finally {
//			if (conn != null) {
//				entity.freeConnection(conn);
//			}
//		}
//		DatastoreInterface.getInstance(entity).setNumberGeneratorValue(entity, valueToSet);
//	}

  protected String getCreationStatement(GenericEntity entity){
  	  if (entity instanceof GroupBMPBean) {
		  System.out.println("GROUP");
	  }

	  IDOEntityField[] pkFields = entity.getEntityDefinition().getPrimaryKeyDefinition().getFields();
  	//StringBuffer returnString = new StringBuffer("CREATE TABLE ").append(entity.getTableName()).append("(");
	String tableName = entity.getEntityDefinition().getSQLTableName();
  	StringBuffer returnString = new StringBuffer(this._dsi.getCreateTableCommand(tableName)).append("(");
  		String[] names = entity.getColumnNames();
		for (int i = 0; i < names.length; i++){
                    String columnName = names[i];
                    returnString.append(getColumnSQLDefinition(columnName,entity));

                    if (i!=names.length-1){
                      returnString.append(",");
                    } else if (pkFields != null && pkFields.length > 0) {
	                		returnString.append(", PRIMARY KEY (");
	                		for (int j = 0; j < pkFields.length; j++) {
	                			if (j != 0) {
	                				returnString.append(",");
	                			}
	                			returnString.append(pkFields[j].getSQLFieldName());
	                		}
	                		returnString.append(")");
                    }
		}
                returnString.append(")");
                //System.out.println(returnString);
		return returnString.toString();
}






  public void deleteEntityRecord(GenericEntity entity)throws Exception{
    deleteTable(entity);
  }

  protected void deleteTable(GenericEntity entity)throws Exception{
		Connection conn= null;
		Statement Stmt= null;
		try{
			conn = entity.getConnection();
			Stmt = conn.createStatement();
			Stmt.executeUpdate("drop table "+entity.getTableName());
		}
		finally{
			if(Stmt != null){
				Stmt.close();
			}
			if (conn != null){
				entity.freeConnection(conn);
			}
		}
  }

  protected void createRefrencedTables(GenericEntity entity)throws Exception{
      /*String[] names = entity.getColumnNames();
      for (int i = 0; i < names.length; i++) {
        String relationShipClass = entity.getRelationShipClassName(names[i]);
        if (!relationShipClass.equals("")) {
          try{
            GenericEntity relationShipEntity = (GenericEntity)Class.forName(relationShipClass).newInstance();
            createEntityRecord(relationShipEntity);
          }
          catch(Exception ex){
            ex.printStackTrace();
          }
        }
      }*/
      List list = getRelatedEntityClasses(entity);
      Iterator iter = list.iterator();
      while (iter.hasNext()) {
        //String className = (String)iter.next();
        Class myClass = (Class)iter.next();
          //try{
            //GenericEntity relationShipEntity = (GenericEntity)Class.forName(className).newInstance();
        	myClass = IDOLookup.getBeanClassFor(myClass);
			GenericEntity relationShipEntity = (GenericEntity)myClass.newInstance();
            createEntityRecord(relationShipEntity);
          //}
          //catch(Exception ex){
          //  ex.printStackTrace();
          //}
      }
  }
  
  protected void createReferencedViews(GenericView entityView)throws Exception{
  	java.util.Collection list = entityView.getDependantViewClasses();
  	Iterator iter = list.iterator();
  	while (iter.hasNext()) {
  		Class myClass = (Class)iter.next();
  		GenericView relationShipView = (GenericView)myClass.newInstance();
  		createEntityView(relationShipView);
  	}
  }

  /**
   * Gets the entities that are related by  one-to many and many-to-many relationships
   * Returns a List of Class Objects
   */
  private List getRelatedEntityClasses(GenericEntity entity){
      List returnNames = new Vector();
      String[] names = entity.getColumnNames();
      for (int i = 0; i < names.length; i++) {
        Class relationShipClass = entity.getRelationShipClass(names[i]);
        if ( relationShipClass!=null ) {
          try{
            returnNames.add(relationShipClass);
          }
          catch(Exception ex){
            ex.printStackTrace();
          }
        }
      }
      returnNames.addAll(getManyToManyRelatedEntityClasses(entity));
      return returnNames;
  }

  /**
   * Gets the entities that are related by many-to-many relationships
   * Returns a List of Class Objects
   */
  private List getManyToManyRelatedEntityClasses(GenericEntity entity){
      List list = new Vector();
      List classList = EntityControl.getManyToManyRelationShipClasses(entity);
      if(classList!=null){
        Iterator iter = classList.iterator();
        while (iter.hasNext()) {
          Class item = (Class)iter.next();
          //String className = item.getName();
          //list.add(className);
          list.add(item);
        }
      }
      return list;
  }

  protected void createMiddleTables(GenericEntity entity)throws Exception{

    //List classList = EntityControl.getManyToManyRelationShipClasses(entity);
    List relationshipList = EntityControl.getManyToManyRelationShips(entity);

    /*
    if(classList==null){
      System.out.println("classList==null for "+entity.getClass().getName());
    }
    if(tableList==null){
      System.out.println("tableList==null for "+entity.getClass().getName());
    }*/

    if(relationshipList!=null){
      //System.out.println("inside 1 for "+entity.getClass().getName());
      //Iterator iter = classList.iterator();
      Iterator relIter = relationshipList.iterator();
      while (relIter.hasNext()) {
        //System.out.println("inside 2 for "+entity.getClass().getName());
        //Class item = (Class)iter.next();
        EntityRelationship relation = (EntityRelationship)relIter.next();
        Map relMap = relation.getColumnsAndReferencingClasses();
        String tableName = relation.getTableName();

        boolean doCreateMiddleTable = !doesTableExist(entity,tableName);

        if(doCreateMiddleTable){
             Set tempSet = relMap.keySet();
             Iterator tempIter = tempSet.iterator();
            while (tempIter.hasNext() && doCreateMiddleTable) {
              String column = (String)tempIter.next();
              Class relClass = (Class)relMap.get(column);
              //GenericEntity entity1 = (GenericEntity)relClass.newInstance();
              GenericEntity entity1 = (GenericEntity)IDOContainer.getInstance().instanciateBean(relClass);
              String referencingTableName = entity1.getTableName();
              doCreateMiddleTable = doesTableExist(entity,referencingTableName);
            }
        }


        //try{
          if(doCreateMiddleTable){
            //String creationStatement = "CREATE TABLE ";
            //creationStatement += tableName;
            String creationStatement = this._dsi.getCreateTableCommand(tableName);
            creationStatement += "(";

            String primaryKeyStatement = this._dsi.getCreatePrimaryKeyStatementBeginning(tableName);

            Set set;
            Iterator iter;

            set = relMap.keySet();
            iter = set.iterator();
            boolean mayAddComma = false;
            while (iter.hasNext()) {
              if(mayAddComma){
                creationStatement += ",";
                primaryKeyStatement += ",";
              }
              String column = (String)iter.next();
              Class relClass = (Class)relMap.get(column);
              //GenericEntity entity1 = (GenericEntity)relClass.newInstance();
              GenericEntity entity1 = (GenericEntity)IDOContainer.getInstance().instanciateBean(relClass);

              //creationStatement += column + " INTEGER NOT NULL";
              creationStatement += column+this.getPrimaryKeyReferenceForManyToManyRelationship(entity1,column,entity1.getIDColumnName());
              primaryKeyStatement +=column;
              mayAddComma = true;
            }
            creationStatement += ")";
            primaryKeyStatement +=")";
            executeUpdate(entity,creationStatement);
            executeUpdate(entity,primaryKeyStatement);


             set = relMap.keySet();
             iter = set.iterator();
            while (iter.hasNext()) {
              String column = (String)iter.next();
              Class relClass = (Class)relMap.get(column);
              //try{
                GenericEntity entity1 = (GenericEntity)IDOLookup.getBeanClassFor(relClass).newInstance();
                //createEntityRecord(entity1);
                createForeignKey(entity,tableName,column,entity1.getTableName(),entity1.getIDColumnName());
              //}
              //catch(Exception e){
              //  e.printStackTrace();
              //}
            }
            
            if (PoolManager.getInstance().hasDatasource(sourceDatasource)) {
//            	IDOHome sourceHome = IDOLookup.getHome(entity.getInterfaceClass(), sourceDatasource);
            	String[] columnNames = _dsi.getColumnNames(tableName, null);
//            	System.out.println(columnNames);
            	
                Statement stmtSource = null;
                ResultSet rsSource = null;
                Statement stmtNew = null;
                Connection connSource = null;
            	Connection connNew = null;
                try {
                    StringBuffer colNameStr = new StringBuffer();
                    for (int i = 0; i< columnNames.length; i++) {
                  	  if (i !=0) {
                  		  colNameStr.append(",");
                  	  }
                  	  colNameStr.append(columnNames[i]);
                    }

                    connSource = ConnectionBroker.getConnection(sourceDatasource);
                	connNew = ConnectionBroker.getConnection();
                  // prepare query
                  String query = "select "+colNameStr.toString()+" from "+tableName;
                  // create a statement
                  stmtSource = connSource.createStatement();
                  // execute query and return result as a ResultSet
                  rsSource = stmtSource.executeQuery(query);
                  // get the column names from the ResultSet
                  
                  int counter = 0;
                  System.out.println("Starting copy ");
                  while (rsSource.next()) {
                	  stmtNew = connNew.createStatement();
                	  StringBuffer b = new StringBuffer();
                	  for (int i = 0; i < columnNames.length; i++) {
                		  if (i != 0) {
                			  b.append(", ");
                		  }
                		  Object val = rsSource.getObject(columnNames[i]);
                		  b.append(val.toString());
                	  }
                	  String sql = "insert into "+tableName+" ("+colNameStr.toString()+") values ("+b.toString()+")";
                	  stmtNew.executeUpdate(sql);
                      stmtNew.close();

            		  if (counter % 10000 == 0) {
            			  if (counter != 0) {
            				  System.out.println(":");
                			  System.out.print(counter+" ");
            			  } else {
            				  System.out.println();
            				  System.out.print("      ");
            			  }
            		  } else if (counter % 1000 == 0) {
            			  System.out.print(":");
            		  } else if (counter % 100 == 0) {
            			  System.out.print(".");
            		  }
            		  ++counter;

                  
                  }
                  
                } catch (Exception e) {
                  e.printStackTrace();
//                  System.exit(1);
                } finally {
                  // release database resources
                  try {
                    rsSource.close();
                    stmtSource.close();
                    ConnectionBroker.freeConnection(connNew);
                    ConnectionBroker.freeConnection(sourceDatasource, connSource);
                  } catch (SQLException e) {
                    e.printStackTrace();
                  }
                }
            	
//            	_dsi.getColumnStringForSelectList(entity, columnName)
//          	  IDOHome localHome = IDOLookup.getHome(entity.getInterfaceClass());
//	      		 GenericEntity ent = (GenericEntity) com.idega.data.GenericEntity.getStaticInstanceIDO(entity.getInterfaceClass(),sourceDatasource);
//	      		 ent.setDatasource(sourceDatasource, false);
            }
        }




          /*
          relatingEntity = (GenericEntity)item.newInstance();
          if(!this.doesTableExist(entity,tableName)){
            String creationStatement = "CREATE TABLE "+tableName+" ( "+entity.getIDColumnName() + " INTEGER NOT NULL,"+relatingEntity.getIDColumnName() + " INTEGER NOT NULL , PRIMARY KEY("+entity.getIDColumnName() + "," + relatingEntity.getIDColumnName() +") )";
            executeUpdate(entity,creationStatement);
            createForeignKey(entity,tableName,entity.getIDColumnName(),entity.getTableName());
            createForeignKey(entity,tableName,relatingEntity.getIDColumnName(),relatingEntity.getTableName());
          }*/
        //}
        //catch(Exception ex){
          //System.err.println("Failed creating middle-table: "+tableName);
          //ex.printStackTrace();
        //}


        //}
        //catch(Exception ex){
        //  System.err.println("Failed creating middle-table: "+tableName);
        //  ex.printStackTrace();
        //}
      }
    }

  }

  protected void createIndexes(GenericEntity entity) throws Exception {
	  	try {
	  		HashMap map = entity.getEntityDefinition().getIndexes();
	  		Set keys = map.keySet();
	  		if (keys != null) {
	  			Iterator iter = keys.iterator();
	  			String key = null;
	  			String[] values = null;
	  			while (iter.hasNext()) {
	  				try {
		  				key = (String) iter.next();
		  				values = (String[]) map.get(key);
//		  				createIndex(entity, key, values);
		  				_dsi.createIndex(entity, key, values);
	  				} catch (Exception e) {
	  					logCopyError(entity, e, "index creation");
//	  					e.printStackTrace();
	  				}
	  			}
	  		}
	  	} catch (NoIndexException ignore) {}
  }
  
  protected void createForeignKeys(IDOEntity entity) throws Exception {
    /*Connection conn = null;
    Statement Stmt = null;
    try {
      conn = entity.getConnection();
      conn.commit();

      String[] names = entity.getColumnNames();
      for (int i = 0; i < names.length; i++) {
        if (!entity.getRelationShipClassName(names[i]).equals("")) {
          Stmt = conn.createStatement();
          int n = Stmt.executeUpdate("ALTER TABLE " + entity.getTableName() + " ADD FOREIGN KEY (" + names[i] + ") REFERENCES " + ((GenericEntity)Class.forName(entity.getRelationShipClassName(names[i])).newInstance()).getTableName() + " ");
          if (Stmt != null) {
            Stmt.close();
          }
        }
      }
    }
    finally {
      if (Stmt != null) {
        Stmt.close();
      }
      if (conn != null) {
        entity.freeConnection(conn);
      }
    }*/
    //String[] names = entity.getColumnNames();
    IDOEntityField[] fields = entity.getEntityDefinition().getFields();
    //for (int i = 0; i < names.length; i++) {
	for (int i = 0; i < fields.length; i++) {
        //try{
          //Class relationShipClass = entity.getRelationShipClass(names[i]);
          //if (relationShipClass!=null) {
          if(fields[i].isPartOfManyToOneRelationship()){
            //String table1=entity.getTableName();

            //Class intefaceClass = IDOLookup.getInterfaceClassFor(relationShipClass);
			Class intefaceClass = fields[i].getManyToOneRelated().getInterfaceClass();
            GenericEntity entityToReference = (GenericEntity)IDOLookup.instanciateEntity(intefaceClass);
            //GenericEntity entityToReference = (GenericEntity)relationShipClass.newInstance();
            //String tableToReference=entityToReference.getTableName();
            //if(!doesTableExist(entity,tableToReference)){
            //  createEntityRecord(entityToReference);
            //}

            //String columnInTableToReference=entityToReference.getIDColumnName();
            //String columnName = names[i];
			String columnName = fields[i].getSQLFieldName();
			try{
				createForeignKey((GenericEntity)entity,entityToReference,columnName);
			}
			catch(Exception e){
				logError("Error Creating foreign key for entity "+entity.getEntityDefinition().getSQLTableName()+" and field "+columnName+". Error message was : "+e.getMessage());
			}
            //createForeignKey(entity,table1,columnName,tableToReference,columnInTableToReference);
          }
        //}
        //catch(Exception ex){
        //  ex.printStackTrace();
        //}
    }
  }
  
  protected void createView(GenericView viewEntity)throws Exception{
  	executeUpdate(viewEntity,viewEntity.getCreationSQL());
  }

  protected void createTable(GenericEntity entity)throws Exception{
    //if(!doesTableExist(entity,entity.getTableName())){
      executeUpdate(entity,getCreationStatement(entity));
    //}
  }

  protected void createTrigger(GenericEntity entity)throws Exception{
    this._dsi.createTrigger(entity);
  }

  protected void createForeignKey(GenericEntity entity,String columnName)throws Exception{
      Class referencingClass = entity.getRelationShipClass(columnName);
      GenericEntity referencingEntity = (GenericEntity)referencingClass.newInstance();
      createForeignKey(entity,referencingEntity,columnName);
  }

  protected void createForeignKey(GenericEntity entity,GenericEntity entityToReference,String columnName)throws Exception{
      createForeignKey(entity,entity.getEntityDefinition().getSQLTableName(),columnName,entityToReference.getEntityDefinition().getSQLTableName(),entityToReference.getIDColumnName());
  }

  protected void createForeignKey(GenericEntity entity,String baseTableName,String columnName, String refrencingTableName)throws Exception{
      createForeignKey(entity,baseTableName,columnName,refrencingTableName,columnName);
  }

  /*protected void createForeignKey(GenericEntity entity,String baseTableName,String columnName, String refrencingTableName,String referencingColumnName)throws Exception{
      String SQLCommand = "ALTER TABLE " + baseTableName + " ADD CONSTRAINT FOREIGN KEY (" + columnName + ") REFERENCES " + refrencingTableName + "(" + referencingColumnName + ")";
      executeUpdate(entity,SQLCommand);
  }*/

  protected void createForeignKey(GenericEntity entity,String baseTableName,String columnName, String refrencingTableName,String referencingColumnName)throws Exception{
      //String SQLCommand = "ALTER TABLE " + baseTableName + " ADD CONSTRAINT FOREIGN KEY (" + columnName + ") REFERENCES " + refrencingTableName + "(" + referencingColumnName + ")";
      //executeUpdate(entity,SQLCommand);
      this._dsi.createForeignKey(entity,baseTableName,columnName,refrencingTableName,referencingColumnName);
  }

  protected void createPrimaryKey(GenericEntity entity,String baseTableName,String columnName, String refrencingTableName,String referencingColumnName)throws Exception{
      //String SQLCommand = "ALTER TABLE " + baseTableName + " ADD CONSTRAINT FOREIGN KEY (" + columnName + ") REFERENCES " + refrencingTableName + "(" + referencingColumnName + ")";
      //executeUpdate(entity,SQLCommand);
      this._dsi.createForeignKey(entity,baseTableName,columnName,refrencingTableName,referencingColumnName);
  }

  protected void updateColumns(GenericEntity entity)throws Exception{
    
  	String[] columnArrayFromDB = getColumnArrayFromDataBase(entity);
    String[] columnArrayFromEntity = entity.getColumnNames();
    for (int i = 0; i < columnArrayFromEntity.length; i++) {
      String column = columnArrayFromEntity[i];
      if(!hasEntityColumn(column,columnArrayFromDB)){
        try{
          addColumn(column,entity);
          if(doesColumnHaveRelationship(column,entity)){
              this.createForeignKey(entity,column);
          }
        }
        catch(Exception e){
          //e.printStackTrace();
        }
      } else { // Checking size !!!
//        EntityAttribute att = entity.getAttribute(column);
//        int size = att.getMaxLength();
//        int oldSize = getColumnSize(column, columnArrayFromDB); 
//        if (!att.isUpdated()) {
//        	// TODO add support to more dataTypes
//	        if (size != oldSize && size > 0 && att.getStorageClass() == String.class && oldSize < 4000 && size < 4000) {
//	        	att.setUpdated(true);
//		        	this._dsi.executeUpdate(entity, "alter table "+entity.getTableName()+" add "+column+"_t varchar("+size+")");
//		        	this._dsi.executeUpdate(entity, "update "+entity.getTableName()+" set "+column+"_t = "+column);
//		        	this._dsi.executeUpdate(entity, "alter table "+entity.getTableName()+" drop "+column);
//		
//		        	this._dsi.executeUpdate(entity, "alter table "+entity.getTableName()+" add "+column+" varchar("+size+")");
//		        	this._dsi.executeUpdate(entity, "update "+entity.getTableName()+" set "+column+" = "+column+"_t");
//		        	this._dsi.executeUpdate(entity, "alter table "+entity.getTableName()+" drop "+column+"_t");
//	        }
//        } else {
//        	System.out.println("Column is updated = "+column);
//        }
        
      }
    }
  }
  
//  private int getColumnSize(String colName, ColumnInfo[] columnsToSearch) {
//  	for (int i = 0; i < columnsToSearch.length; i++) {
//  		if (columnsToSearch[i].getColumnName().equalsIgnoreCase(colName)) {
//  			return columnsToSearch[i].getColumnSize();
//  		}
//  	}
//  	return -1;
//  }
  
  private boolean compareIndexColumns(String[] arr1, String[] arr2) {
  		if (arr1 != null && arr2 != null && arr1.length == arr2.length) {
  			boolean returner = true;
  			for (int i = 0; i < arr1.length && returner; i++) {
  				returner = false;
  				for (int j = 0; j < arr2.length && !returner; j++) {
  					if (arr1[i].equals(arr2[j])) {
  						returner = true;
  					}
  				}
  				
  				if (!returner) {
  					return returner;
  				}
  			}
  			return true;
  		}
  		return false;
  }

  private void updateTriggers(GenericEntity entity) {
		try {
		this._dsi.updateTriggers(entity, true);
	}
	catch (Exception e) {
		e.printStackTrace();
	}
}

  private void updateIndexes(GenericEntity entity) {
  		if (this._dsi.useIndexes()) {
  			HashMap indexesFromDB = this._dsi.getTableIndexes(entity.getDatasource(), entity.getTableName());
	  		try {
					HashMap map = entity.getEntityDefinition().getIndexes();
					Set indexesFromEntity = map.keySet();
					Set setFromDB = indexesFromDB.keySet();
					
					
					// Removing keys from map that exist in DB
					Iterator dbKeyIter = setFromDB.iterator();
					String dbKey;
					while (dbKeyIter.hasNext()) {
						dbKey = (String) dbKeyIter.next();
						if (map.containsKey(dbKey)) {
							map.remove(dbKey);
						}
					}

					// Removing columns from map that are already indexed
					if (indexesFromDB != null && !indexesFromDB.isEmpty()) {
						try {
							HashMap tempMap = new HashMap(map);
							Iterator dbValues = indexesFromDB.values().iterator();
							Iterator entityKeys;// = tempMap.keySet().iterator();
							String[] columns = null;
							while (dbValues.hasNext()) {
								columns = (String[]) dbValues.next();
								entityKeys = tempMap.keySet().iterator();
								while (entityKeys.hasNext()) {
									String key = (String) entityKeys.next();
									if (compareIndexColumns(columns, (String[]) map.get( key ))) {
										map.remove(key);
									}
								}
							}
						
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
						
					// CREATING
					Iterator iter = indexesFromEntity.iterator();
					String indexName;
					while (iter.hasNext()) {
						indexName = (String) iter.next();
						try {
							_dsi.createIndex(entity, indexName, (String[]) map.get(indexName));
//							this.createIndex(entity, indexName, (String[]) map.get(indexName));
						}
						catch (Exception e1) {
							logError("[IDOTableCreator] : failed to create index : "+indexName+" ("+e1.getMessage()+")");
						}
					}
					// REMOVING - Not active since it tries to removed the PRIMARY_KEY index....
					/*iter = indexesFromDB.iterator();
					while (iter.hasNext()) {
						indexName = (String) iter.next();
						try {
							dropIndex(entity, indexName);
						} catch (Exception e1) {
							System.out.println("IDOTableCreator : failed to drop index : "+indexName);
						}
					}*/
					
				}
				catch (NoIndexException e) {
					// REMOVING ALL INDEXES - Not active since it tries to removed the PRIMARY_KEY index....
					/*
					if (indexesFromDB != null) {
						Iterator iter = indexesFromDB.iterator();
						String indexName;
						while (iter.hasNext()) {
							indexName = (String) iter.next();
							try {
								dropIndex(entity, indexName);
							} catch (Exception e1) {
								System.out.println("IDOTableCreator : failed to drop index : "+indexName);
							}
						}
					}*/
				}
  		}

  }

  
  private String[] getColumnArrayFromDataBase(GenericEntity entity){
  	return this._dsi.getTableColumnNames(entity.getDatasource(),entity.getTableName());
  }

  private boolean hasEntityColumn(String columnName,String[] columnsFromDB){
  	String currentColumn = null;
    if(columnsFromDB!=null){
      for (int i = 0; i < columnsFromDB.length; i++) {
        currentColumn = columnsFromDB[i];
        if(currentColumn.equalsIgnoreCase(columnName)){
          return true;
        }
      }
    }
    return false;
  }

  private void addColumn(String columnName,GenericEntity entity)throws Exception{
    //String SQLString = "alter table "+entity.getTableName()+" add "+getColumnSQLDefinition(columnName,entity);
    String SQLString = this._dsi.getAddColumnCommand(columnName,entity);
  	executeUpdate(entity,SQLString);
  }

  protected String getColumnSQLDefinition(String columnName,GenericEntity entity){
  	return this._dsi.getColumnSQLDefinition(columnName,entity);
  }

  private boolean doesColumnHaveRelationship(String columnName,GenericEntity entity){
    return (entity.getRelationShipClass(columnName)!=null);
  }

  protected String getPrimaryKeyReferenceForManyToManyRelationship(GenericEntity entity,String column,String referencingColumn){
    try{
      EntityAttribute attr = entity.getAttribute(referencingColumn);
      Class storageClass = attr.getStorageClass();
      int maxLength = attr.getMaxLength();
      String sqlType = this._dsi.getSQLType(storageClass,maxLength);
      return " "+sqlType + " NOT NULL";
    }
    catch(NullPointerException ne){
      ne.printStackTrace();
      System.err.println("---");
      System.err.println("Nullpointer where entity="+entity.getClass().getName()+" and column="+column);
      System.err.println("---");
      return " INTEGER NOT NULL";
    }
  }
  
  //STANDARD LOGGING METHODS:
  
  /**
   * Logs out to the default log level (which is by default INFO)
   * @param msg The message to log out
   */
  protected void log(String msg) {
  	//System.out.println(string);
  	getLogger().log(getDefaultLogLevel(),msg);
  }

  /**
   * Logs out to the error log level (which is by default WARNING) to the default Logger
   * @param e The Exception to log out
   */
  protected void log(Exception e) {
  	LoggingHelper.logException(e,this,getLogger(),getErrorLogLevel());
  }
  
  /**
   * Logs out to the specified log level to the default Logger
   * @param level The log level
   * @param msg The message to log out
   */
  protected void log(Level level,String msg) {
  	//System.out.println(msg);
  	getLogger().log(level,msg);
  }
  
  /**
   * Logs out to the error log level (which is by default WARNING) to the default Logger
   * @param msg The message to log out
   */
  protected void logError(String msg) {
  	//System.err.println(msg);
  	getLogger().log(getErrorLogLevel(),msg);
  }

  /**
   * Logs out to the debug log level (which is by default FINER) to the default Logger
   * @param msg The message to log out
   */
  protected void logDebug(String msg) {
  	//System.err.println(msg);
  	getLogger().log(getDebugLogLevel(),msg);
  }
  
  /**
   * Logs out to the SEVERE log level to the default Logger
   * @param msg The message to log out
   */
  protected void logSevere(String msg) {
  	//System.err.println(msg);
  	getLogger().log(Level.SEVERE,msg);
  }	
  
  
  /**
   * Logs out to the WARNING log level to the default Logger
   * @param msg The message to log out
   */
  protected void logWarning(String msg) {
  	//System.err.println(msg);
  	getLogger().log(Level.WARNING,msg);
  }
  
  /**
   * Logs out to the CONFIG log level to the default Logger
   * @param msg The message to log out
   */
  protected void logConfig(String msg) {
  	//System.err.println(msg);
  	getLogger().log(Level.CONFIG,msg);
  }	
  
  /**
   * Logs out to the debug log level to the default Logger
   * @param msg The message to log out
   */
  protected void debug(String msg) {
  	String logMsg = "[idoTableCreator] : "+msg;
  	logDebug(logMsg);
  }	
  
  /**
   * Gets the default Logger. By default it uses the package and the class name to get the logger.<br>
   * This behaviour can be overridden in subclasses.
   * @return the default Logger
   */
  protected Logger getLogger(){
  	return Logger.getLogger(this.getClass().getName());
  }
  
  /**
   * Gets the log level which messages are sent to when no log level is given.
   * @return the Level
   */
  protected Level getDefaultLogLevel(){
  	return Level.INFO;
  }
  /**
   * Gets the log level which debug messages are sent to.
   * @return the Level
   */
  protected Level getDebugLogLevel(){
  	return Level.FINER;
  }
  /**
   * Gets the log level which error messages are sent to.
   * @return the Level
   */
  protected Level getErrorLogLevel(){
  	return Level.WARNING;
  }
  
  //ENTITY SPECIFIC LOG MEHTODS:
  
  ///**
  // * This method outputs the outputString to System.out if the Application property
  // * "debug" is set to "TRUE"
  // */
  //public void debug(String outputString) {
  //	if (isDebugActive()) {
  //		//System.out.println("[DEBUG] \"" + outputString + "\" : " + this.getEntityName());
  //	}
  //}
  /**
   * This method logs the sqlCommand if the Log Level is low enough 
   */
  public void logSQL(String sqlCommand) {
  	log(Level.FINEST,sqlCommand);
  	//if (isDebugActive()) {
  	//System.out.println("[DEBUG] \"" + outputString + "\" : " + this.getEntityName());
  	//}
  }
  
	protected boolean isDebugActive() {
		return getIWMainApplication().getSettings().isDebugActive();
	}
	
	public IWMainApplication getIWMainApplication(){
		return IWMainApplication.getDefaultIWMainApplication();
	}
  //END STANDARD LOGGING METHODS

	protected void logCopyError(GenericEntity entity, Exception exception, String extraInfo) {
		try{
			if (useCopyLog) {
				File file = new File(getIWMainApplication().getApplicationRealPath()+"/copyErrorLog.txt");
				FileUtil.createFileIfNotExistent(file);
				FileWriter fstream = new FileWriter(file, true);
				BufferedWriter out = new BufferedWriter(fstream);
				StackTraceElement[] elems = exception.getStackTrace();
				out.write("=====================================================================================================\n");
				out.write(entity.getEntityName() +" ("+entity.getPrimaryKey()+") : "+extraInfo+"\n");
				out.write(exception.getMessage() +"\n");
				for (int i = 0; i < elems.length && i < 15; i++) {
					out.write("    "+elems[i] +"\n");
				}
				out.write("=====================================================================================================\n");
				out.close();

				System.out.println(">>>> COPY ERROR, logged to "+file.getAbsolutePath());
			} else {
				getLogger().log(getErrorLogLevel(), "Error", exception);
			}
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}

}
