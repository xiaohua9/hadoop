# hadoop
使用Java操作HDFS



## 附：总结一个高可用分布式集群搭建实例

| 主机    | IP   |      |      |      |      |      |      |      |
| ------- | ---- | :--- | ---- | ---- | ---- | ---- | ---- | ---- |
| hadoop1 | 101  | nn   |      |      | zkfc | jnn  |      |      |
| hadoop2 | 102  | nn   | dn   | zk   | zkfc | jnn  |      | nm   |
| hadoop3 | 103  |      | dn   | zk   |      | jnn  | rm   | nm   |
| hadoop4 | 104  |      | dn   | zk   |      |      | rm   | nm   |

**1、安装centos**

- 在这个机器上的/opt下创建一个soft目录，用于存放软件安装包，把Hadoop、zookeeper、jdk安装包放在soft下，再解压到/opt下，改名为jdk、hadoop、zookeeper

- 配置环境变量

  jdk:

  ```xml
  export JAVA_HOME=/opt/jdk
  export PATH=$JAVA_HOME/bin:$PATH
  ```

  hadoop:

  ```xm
  export HADOOP_HOME=/opt/hadoop
  export PATH=$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$PATH
  ```

  zookeeper:

  ```xml
  export ZOOKEEPER_HOME=/opt/zookeeper
  export PATH=$ZOOKEEPER_HOME/bin:$PATH
  ```

  生效环境变量配置：source /etc/profile

**2、克隆hadoop1、hadoop2、hadoop3、hadoop4**

- 分别在四台电脑上执行以下操纵

  - 关闭防火墙：chkconfig iptables off

  - /etc/sysconfig/selinux文件里的selinux变量设为disabled

  - 删掉/etc/udev/rules.d/70-persistent-net.rules文件

  - 改主机名：vim /etc/sysconfig/network          hostname=hadoop1

  - 改映射：vim /etc/hosts  

    ​			xxx.xxx.xxx.101 haoop1

    ​			xxx.xxx.xxx.102 haoop2

    ​			xxx.xxx.xxx.103 haoop3

    ​			xxx.xxx.xxx.104 haoop4

  - 改IP: vim /etc/sysconfig/network-script/ifcfg-eth0

  ```xml
  DEVICE=eth0
  #HWADDR=xx:xx:xx:xx:xx:xx
  TYPE=Ethernet
  #UUID=xxxxxx-xxxxx-xxxxxx-xxxxxx-xxxxxxxxxx
  ONBOOT=yes
  NM_CONTROLLED=yes
  BOOTPROTO=static
  IPADDR=xxx.xxx.xxx.101
  NETMASK=255.255.255.0
  GATEWAY=xxx.xxx.xxx.2
  DNS1=8.8.8.8
  ```

**3、四台机器两两免密**

- 在每台机器上执行

  - 生成.ssh目录：ssh hadoop1/2/3/4(先在每台机器上执行一次)
  - 生成公钥文件id_dsa.pub：ssh-keygen -t dsa -P '' -f ~/.ssh/id_dsa  
  - 复制改名公钥文件：cp id_dsa.pub hadoop1
  - 将改名的hadoop1公钥文件分发到hadoop2/3/4机器：scp hadoop1 hadoop2:/root/.ssh/

- 实现免密

  此时每台机器上的/root/.ssh下都有hadoop1/2/3/4这是个公钥文件,在每台机器上执行以下语句

  - cat hadoop1 >> authorized_keys 
  - cat hadoop2 >> authorized_keys 
  - cat hadoop3 >> authorized_keys 
  - cat hadoop4 >> authorized_keys 

- 使用ssh hadoop1/2/3/4 测试，看是否能够直接登录

**4、hadoop配置**

- 删除doc文件夹：rm -rf /opt/hadoop/share/doc/

- 在/opt/hadoop/下：mkdir -p ha/jn

- 将/opt/hadoop/etc/hadoop/下hadoop-env.sh和mapred-env.sh和yarn-env.sh三个文件的JAVA_HOME变量改为/opt/jdk

- 同上一步的目录下

  - vim core-site.xml

  ```xml
  <configuration>
   <property>
          <name>fs.defaultFS</name>
          <value>hdfs://mycluster</value>
   </property>
  <property>
          <name>hadoop.tmp.dir</name>
          <value>/opt/hadoop/ha</value>
  </property>
  <property>
     <name>ha.zookeeper.quorum</name>
     <value>hadoop2:2181,hadoop3:2181,hadoop4:2181</value>
  </property>
  </configuration>
  ```

  - vim hdfs-site.xml

    ```xml
    <configuration>
    	<property>
    		<name>dfs.replication</name>
    		<value>2</value>
    	</property>
    <property>
      <name>dfs.nameservices</name>
      <value>mycluster</value>
    </property>
    <property>
      <name>dfs.ha.namenodes.mycluster</name>
      <value>nn1,nn2</value>
    </property>
    <property>
      <name>dfs.namenode.rpc-address.mycluster.nn1</name>
      <value>hadoop1:8020</value>
    </property>
    <property>
      <name>dfs.namenode.rpc-address.mycluster.nn2</name>
      <value>hadoop2:8020</value>
    </property>
    <property>
      <name>dfs.namenode.http-address.mycluster.nn1</name>
      <value>hadoop1:50070</value>
    </property>
    <property>
      <name>dfs.namenode.http-address.mycluster.nn2</name>
      <value>hadoop2:50070</value>
    </property>
    <property>
      <name>dfs.namenode.shared.edits.dir</name>
      <value>qjournal://hadoop1:8485;hadoop2:8485;hadoop3:8485/mycluster</value>
    </property>
    <property>
      <name>dfs.client.failover.proxy.provider.mycluster</name>
    <value>org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider</value>
    </property>
    <property>
      <name>dfs.ha.fencing.methods</name>
      <value>sshfence</value>
    </property>
    <property>
      <name>dfs.ha.fencing.ssh.private-key-files</name>
      <value>/root/.ssh/id_dsa</value>
    </property>
    <property>
      <name>dfs.journalnode.edits.dir</name>
      <value>/opt/hadoop/ha/jn</value>
    </property>
    <property>
       <name>dfs.ha.automatic-failover.enabled</name>
       <value>true</value>
     </property>
    </configuration>
    ```

  - vim slaves(设置数据节点)

  ```xml
  hadoop2
  hadoop3
  hadoop4
  ```

- 分发/opt/hadoop:

  scp hadoop hadoop2:/opt/

  scp hadoop hadoop3:/opt/

  scp hadoop hadoop4:/opt/

**5、zookeeper集群配置（在hadoop2上配置再分发给3和4）**

- mkdir /opt/zookeeper/zk

- /opt/zookeeper/conf/下的zooxxx改为zoo.cfg,再vim zoo.cfg

  - 将变量dataDir值改为：/opt/zookeeper/zk
  - 添加：

  ```xml
  server.1=hadoop2:2888:3888
  server.2=hadoop3:2888:3888
  server.3=hadoop4:2888:3888
  ```

- 分发/opt/zookeeper

   ```xml
  scp zookeeper hadoop3:/opt/
  scp zookeeper hadoop4:/opt/
  ```

- 配置myid文件

  ```xml
  在hadoop2这个服务器上的zk这个目录中创建一个myid的文件
  echo 1 >> /opt/zookeeper/zk/myid 
  在hadoop3这个服务器上的zk这个目录中创建一个myid的文件
  echo 2 >> /opt/zookeeper/zk/myid 
  在hadoop4这个服务器上的zk这个目录中创建一个myid的文件
  echo 3 >> /opt/zookeeper/zk/myid 
  ```

  ---

  ### 高可用分布式集群配置完毕

  ---

**6、启动**

- 在hadoop2/3/4启动zookeeper集群

  ```xml
  zkServer.sh start  在三台服务器上启动
  zkServer.sh stop 停止服务
  zkServer.sh status  状态
  zkServer.sh stop leader 的服务器 
  ```

-  在hadoop1/2/3启动journalnode

  ```xml
  journalnode要在hadoop集群启动前先启动
  在hadoop1 hadoop2 hadoop3 上先启动  [以后会自动跟随hdfs一起启动]
  hadoop-daemon.sh start journalnode
  ```

- 格式化namenode节点

  ```xml
  在hadoop1节点上
  hdfs namenode -format
  在hadoop1上启动namenode
  hadoop-daemon.sh start namenode
  在hadoop2上同步
  hdfs namenode -bootstrapStandby  完成对主节点的信息的copy
  ```

- 格式化zkfc(在hadoop1)

  ​	hdfs zkfc -formatZK

- 在hadoop1和hadoop2：yum install psmisc -y  (这是下载，请连接网络)

- hadoop1启动集群：start-dfs.sh

- 案例测试：

  ```xml
  hadoop-daemon.sh stop namenode 关闭node01的进程  node02变active
  hadoop-daemon.sh start namenode
  hdfs haadmin -getServiceState nn1  查看nn1的状态
  也可以在浏览器查看状态：hadoop1:50070
  ```

- hadoop1关闭集群：stop-dfs.sh 

---

### 配置resourcemanager的高可用

---

cd /opt/hadoop/etc/hadoo/

- vim mapred-site.xml配置文件

  ```xml
  <configuration>
  	<property>
  	<name>mapreduce.framework.name</name>
      <value>yarn</value>
  </property>
  </configuration>
  ```

- vim yarn-site.xml

  ```xml
  <configuration>
  <property>
  	<name>yarn.nodemanager.aux-services</name>
      <value>mapreduce_shuffle</value>
  </property>
  <property>
    <name>yarn.resourcemanager.recovery.enabled</name>
    <value>true</value>
  </property>
  <property>
    <name>yarn.resourcemanager.cluster-id</name>
    <value>yarn-ha</value>
  </property>
  <property>
    <name>yarn.resourcemanager.ha.rm-ids</name>
    <value>rm1,rm2</value>
  </property>
  <property>
    <name>yarn.resourcemanager.hostname.rm1</name>
    <value>hadoop3</value>
  </property>
  <property>
    <name>yarn.resourcemanager.hostname.rm2</name>
    <value>hadoop4</value>
  </property>
  
  <property>
      <name>yarn.resourcemanager.webapp.address.rm1</name>
      <value>hadoop3:8088</value>
  </property>
  <property>
      <name>yarn.resourcemanager.webapp.address.rm2</name>
      <value>hadoop4:8088</value>
  </property>
  
  <property>
    <name>yarn.resourcemanager.store.class</name>
  <value>org.apache.hadoop.yarn.server.resourcemanager.recovery.ZKRMStateStore</value>
  </property>
  <property>
    <name>yarn.resourcemanager.zk-address</name>
    <value>hadoop2:2181,hadoop3:2181,hadoop4:2181</value>
  </property>
  <!--Configurations for HA of ResourceManager-->
  <property>
    <name>yarn.resourcemanager.ha.enabled</name>
    <value>true</value>
  </property>
  </configuration>
  ```

- 分发：将mapred-site.xml与yarn-site.xml分发到另外三台服务器上

- 启动整个集群

  ```xml
  1.启动zookeeper集群  zkServer.sh start  分别在hadoop2,hadoop3以及hadoop4上执行
  2.启动hadoop集群     start-dfs.sh 在hadoop1上执行
  3.启动yarn  start-yarn.sh        在hadoop1上执行 
  4.启动rm    yarn-daemon.sh start resourcemanager hadoop2和hadoop3上执行
  2.3可以使用stall-all.sh 来代替
  
  可以在浏览器登录hadoop3:8088查看resourcemanager
  ```

- 运行一个wordcount的案例

  ```xml
  1.hdfs dfs -mkdir  /input  创建一个目录 
  2.hdfs dfs -put a.txt /input/  将一个英文的文本上传到这个目录
  3.hdfs dfs -mkdir /output  创建一个输出目录 
     cd /opt/hadoop/share/hadoop/mapreduce
  4.hadoop jar hadoop-mapreduce-examples-2.7.6.jar WordCount /input  /output 
  ```

  ---

  ### 在集群中搭建远程hive
  
  ---
  
-   我们选择hadoop1安装MySQL，作为元数据的保存节点，hadoop2作为metastore服务节点，hadoop3/4作为客户端

- 在hadoop1上安装MySQL 

  ```xml
  yum install mysql-server -y 
  service mysqld start mysql 
  -uroot use mysql 
  GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY 'root' WITH GRANT OPTION; 
  flush privileges; 
  CREATE USER 'hive'@'%' IDENTIFIED BY '123';
  grant all privileges on hive_meta.* to hive@"%" identified by '123';
  flush privileges;
  ```

- 在hadoop2上操作

  ```xml
  1、把hive解压到/opt/下
  2、export HIVE_HOME=/opt/hive 
     export PATH=$HIVE_HOME/bin:$PATH
  3、source /etc/profile 生效
  4、mysql的驱动jar包拷贝到$HIVE_HOME/lib目录下
  ```

  - 进入到hive的conf目录中修改 mv hive-default.xml.template hive-site.xml

    ```xml
    <configuration>
    <property>
    	<name>hive.metastore.warehouse.dir</name>
    	<value>/user/hive_remote/warehouse</value>
    </property>
    <property>
    	<name>javax.jdo.option.ConnectionURL</name>
    	<value>jdbc:mysql://hadoop1/hive_remote?createDatabaseIfNotExist=true</value>
    </property>
    <property>
    	<name>javax.jdo.option.ConnectionDriverName</name>
    	<value>com.mysql.jdbc.Driver</value>
    </property>
    <property>
    	<name>javax.jdo.option.ConnectionUserName</name>
    	<value>root</value>
    </property>
    <property>
    	<name>javax.jdo.option.ConnectionPassword</name>
    	<value>root</value>
    </property>
    </configuration>
    ```

- 把hadoop2的/opt/hive分发给hadoop3和4

- 在hadoop3上

  - 设置环境变量

  ```xml
  export HIVE_HOME=/opt/hive 
  export PATH=$HIVE_HOME/bin:$PATH
  source /etc/profile 生效
  ```

  - vim /opt/hive/conf/hive-site.xml

  ```xml
  <configuration>
  <property>
  	<name>hive.metastore.warehouse.dir</name>
  	<value>/user/hive_remote/warehouse</value>
  </property>
  <property>
  	<name>hive.metastore.uris</name>
  	<value>thrift://hadoop2:9083</value>
  </property>
  </configuration>
  ```

- hadoop4同hadoop4配置

- 在hadoop2上后台启动元数据服务：hive --service metastore &

- hadoop3和4就能正常进入hive了

- 验证：此时hadoop1数据库会有一个hive_remote数据库，保存表的元数据，可以查看，同时hdfs也会产生/user/hive_remote/warehouse/用于存放主数据，至于hdfs的概念这里就不说了