public class HDFSTest {
    private FileSystem fs;
    @Before
    public void before() throws Exception{
        Configuration configuration=new Configuration();
        configuration.set("HADOOP_USER_NAME","root");
        fs=FileSystem.get(configuration);
    }



    @Test
    public void mkdirTest() throws Exception{
        Path path=new Path("/hadoop");
        if(!fs.exists(path))
        {
            fs.mkdirs(path);
        }
        System.out.println("目录创建成功!");
    }
    //hdfs dfs -put xxxx /hadoop

    @Test
    public void uploadTest() throws  Exception{
        Path ipath=new Path("d:\\person.xml");
        Path opath=new Path("/hadoop");
        fs.copyFromLocalFile(ipath,opath);
        System.out.println("copy ok");
    }

    @Test
    public void downloadTest() throws  Exception{

        Path opath=new Path("/hadoop/person.xml");
        FSDataInputStream in = fs.open(opath);
        File file=new File("f:\\person.xml");
        FileOutputStream out=new FileOutputStream(file);
        IOUtils.copyBytes(in,out,1024);

    }

    @Test
    public void getDetailsTest()throws  Exception{
        Path path=new Path("/hadoop/person.xml");
        FileStatus fileStatus = fs.getFileStatus(path);
        System.out.println(fileStatus.getBlockSize());
        System.out.println(fileStatus.getReplication());
        System.out.println(fileStatus.getLen());
        System.out.println(fileStatus.getPath());
        System.out.println("-----------------------------------");
        Path path1=new Path("/hadoop");
        FileStatus[] fileStatuses = fs.listStatus(path1);
        for(FileStatus fs :fileStatuses){
            System.out.println(fs.getBlockSize());
            System.out.println(fs.getReplication());
            System.out.println(fs.getLen());
            System.out.println(fs.getPath());
        }
    }

    @Test
    public void getBlocksTest()throws  Exception{
        Path path=new Path("/hadoop");
        RemoteIterator<LocatedFileStatus> it = fs.listFiles(path, true);
        while(it.hasNext()){
            LocatedFileStatus lfs= it.next();
            BlockLocation[] blockLocations = lfs.getBlockLocations();
            for(BlockLocation bl:blockLocations){

                String[] hosts=bl.getHosts();
                for(String h:hosts){
                    System.out.println(h);
                }
                String [] names=bl.getNames();
                for(String n:names){
                    System.out.println(n);
                }
            }
        }
    }
}
