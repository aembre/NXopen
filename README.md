# NXopen
基于Java语言的NX二次开发实例

本例为maven工程，pom中的jar需自己安装，到{NX安装目录}\NXBIN目录下找到指定jar包，参考以下命令本地安装jar。

```
mvn install:install-file -DgroupId=com.seimens -DartifactId=NXOpen -Dversion=11.0 -Dpackaging=jar -Dfile="C:\Siemens\NX11\NXBIN\NXOpen.jar"
```

安装完成后即可使用配置引用jar

```
<dependency>
    <groupId>com.seimens</groupId>
    <artifactId>NXOpen</artifactId>
    <version>11.0</version>
</dependency>
```

NX Java开发环境搭建以及NX项目部署等详见博客：[NX二次开发](https://aembre.github.io/categories/NX%E4%BA%8C%E6%AC%A1%E5%BC%80%E5%8F%91/)

- #### 1.批量计算方钢尺寸并写入到属性

  1.1 目的Objective

  批量计算方钢尺寸，并写入到对应属性。

  1.2 功能说明 Comments Description

  打开一个总装配后，执行批量计算方钢尺寸的命令，将所有符合方钢特征的模型进行计算，得出材料和长度，分别写到不同的属性中。

  1.3依赖关系 Dependencies

  - 子件中的模型需唯一，否则不计算

  - 不是方钢特征的模型不计算

  1.4输入 Input

  - 打开一个装配

  - 该装配中的子件很多有方钢模型

  1.5输出Output

  - 有正确方钢模型的子件的属性已自动计算并填写正确

  1.6 逻辑 Logic and Key Control

  - 方钢的模型特征为矩形体模型并中间抽壳，两边可能平整，也可能不平整
  - 矩形体的长、宽、抽壳厚度作为【材料】属性，格式为50\*50\*5
  - 矩形体的高作为【备注】属性，格式为1100mm，取整，如果矩形体两边不平整，按垂直矩形体的截面方向取模型的长度最大值

  1.7 验证Verification

  1. 打开装配
  2. 执行命令
  3. 查看属性【材料】、【备注】是否已填写
  4. 手工测量模型验证属性值是否正确