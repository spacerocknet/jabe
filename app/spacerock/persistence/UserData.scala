package spacerock.persistence

import java.util.Date
import java.util.Random
import com.netflix.astyanax.AstyanaxContext
import com.netflix.astyanax.Keyspace
import com.netflix.astyanax.connectionpool.NodeDiscoveryType
import com.netflix.astyanax.connectionpool.OperationResult
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl
import com.netflix.astyanax.connectionpool.impl.CountingConnectionPoolMonitor
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl
import com.netflix.astyanax.model.CqlResult
import com.netflix.astyanax.model.Row
import com.netflix.astyanax.MutationBatch
import com.netflix.astyanax.model.Column
import com.netflix.astyanax.model.ColumnFamily
import com.netflix.astyanax.model.ColumnList
import com.netflix.astyanax.serializers.IntegerSerializer
import com.netflix.astyanax.serializers.StringSerializer
import com.netflix.astyanax.thrift.ThriftFamilyFactory
import scaldi.{Injectable, Injector}
import models.Subscriber

trait UserDataDAO {
  def insertRandom() : Unit
  def insertNewUserWithNoInfo(user: Subscriber) : Boolean
  def retrieveUser(uid: String) : Subscriber
}


class SubscriberDataDAO(implicit inj: Injector) extends UserDataDAO with Injectable {
    val cluster = inject [String] (identified by "cassandra.cluster")   
  
    val context = new AstyanaxContext.Builder()
               .forCluster(cluster)
               .forKeyspace("spacerock")
               .withAstyanaxConfiguration(new AstyanaxConfigurationImpl()      
                                           .setDiscoveryType(NodeDiscoveryType.RING_DESCRIBE))
               .withConnectionPoolConfiguration(new ConnectionPoolConfigurationImpl("MyConnectionPool")
                                                 .setPort(9160)
                                                 .setMaxConnsPerHost(1)
                                                 .setSeeds("127.0.0.1:9160"))
               .withAstyanaxConfiguration(new AstyanaxConfigurationImpl()      
                                           .setCqlVersion("3.0.0")
                                           .setTargetCassandraVersion("1.2"))
               .withConnectionPoolMonitor(new CountingConnectionPoolMonitor())
                                           .buildKeyspace(ThriftFamilyFactory.getInstance());
    context.start()
    
    var keyspace = context.getClient();

    val randomGenerator = new Random();
    val CF =  ColumnFamily.newColumnFamily("UserData", StringSerializer.get(), StringSerializer.get())
  
    
    def insertRandom() : Unit =  {
        val m:MutationBatch  = keyspace.prepareMutationBatch();

        m.withRow(CF, "abcde")
         .putColumn("col1", "val1", null)
         .putColumn("col2", "val2", null)
         .putColumn("col3", "val3", null)
     
       val result = m.execute() //OperationResult<Void>
    }

    def insertNewUserWithNoInfo(user: Subscriber) : Boolean = {
        val m:MutationBatch  = keyspace.prepareMutationBatch();
        
         m.withRow(CF, "uid." + user.uuid)
          .putColumn("platform", user.platform, null)
          .putColumn("model", user.model, null)
          .putColumn("os", user.os, null)
          .putColumn("phone", user.phone, null)
          .putColumn("deviceUuid", user.deviceUuid, null)
        
        m.execute()  
        true
    }
    
    def retrieveUser(uid: String) : Subscriber = {  
         val columns: ColumnList[String] = keyspace.prepareQuery(CF)
                                                  .getKey(uid)
                                                  .execute().getResult();
         
         if (!columns.isEmpty()) {
             val user = new Subscriber(uid, 
                                       columns.getColumnByName("platform").getStringValue(),
                                       columns.getColumnByName("os").getStringValue(),
                                       columns.getColumnByName("model").getStringValue(),
                                       columns.getColumnByName("phone").getStringValue(),
                                       columns.getColumnByName("deviceUuid").getStringValue())
                                       
             
             return user
         }
         
         return null
    }
}