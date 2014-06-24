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

trait TAppsConfig {
  def getAppsConfiguration(uid: String) : AppsConfig
}


case class AppsConfig(val appsName: String, val categories: String) {
    
}

class AppsConfigDAO(implicit inj: Injector) extends TAppsConfig with Injectable {
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


    val CF =  ColumnFamily.newColumnFamily("AppsConfig", StringSerializer.get(), StringSerializer.get())
  
    
    def getAppsConfiguration(apps: String) : AppsConfig = {  
         val columns: ColumnList[String] = keyspace.prepareQuery(CF)
                                                  .getKey(apps)
                                                  .execute().getResult();
         
         if (!columns.isEmpty()) {
             val appsConf = new AppsConfig(apps, 
                                       columns.getColumnByName("categories").getStringValue())
                                       
             
             return appsConf
         }
         
         return null
    }
}