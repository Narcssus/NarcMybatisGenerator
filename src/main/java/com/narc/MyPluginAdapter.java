package com.narc;

import com.narc.utils.FileUtils;
import org.mybatis.generator.api.*;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.XmlConstants;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author : Narcssus
 * @date : 2020/3/7 20:43
 */
public class MyPluginAdapter extends PluginAdapter {

    private String targetPackage;
    private String targetProject;
    private String targetProjectXml;
    private String baseDir;
    private String targetPackageXml;
    private String targetPackageService;

    private String author;
    private SimpleDateFormat dateFormatter;
    private boolean isOverWrite;
    private String tagString;

    private String mapperName;
    private String mapperExtendName;
    private String daoServiceName;


    @Override
    public boolean validate(List<String> list) {
        author = properties.getProperty("author", "NarcMybatisGenerator");
        dateFormatter = new SimpleDateFormat(properties.getProperty("dateFormat", "yyyy-MM-dd"));
        isOverWrite = Boolean.parseBoolean(properties.getProperty("isOverWrite", "false"));
        tagString = properties.getProperty("tagString", "该注释以下的内容不会被覆盖，请不要删除或修改此条注释内容");

        baseDir = properties.getProperty("baseDir");
        targetPackage = properties.getProperty("targetPackage");
        targetProject = properties.getProperty("targetProject");
        targetProjectXml = properties.getProperty("targetProjectXml");
        targetPackageXml = properties.getProperty("targetPackageXml");
        targetPackageService = properties.getProperty("targetPackageService");

        if (baseDir == null || baseDir.trim().isEmpty()
                || targetPackage == null || targetPackage.trim().isEmpty()
                || targetProject == null || targetProject.trim().isEmpty()
                || targetProjectXml == null || targetProjectXml.trim().isEmpty()
                || targetPackageXml == null || targetPackageXml.trim().isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        //import
        topLevelClass.addImportedType("lombok.Data");
        topLevelClass.addImportedType("javax.validation.constraints.*");
        //注解
        topLevelClass.addAnnotation("@Data");
        return true;
    }

    @Override
    public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
        //Mapper文件的注释
        interfaze.addJavaDocLine("/**");
        interfaze.addJavaDocLine(" * @author " + author);
        interfaze.addJavaDocLine(" * @date " + dateFormatter.format(new Date()));
        interfaze.addJavaDocLine("*/");
        interfaze.addAnnotation("@Mapper");
        interfaze.addImportedType(new FullyQualifiedJavaType("org.apache.ibatis.annotations.Mapper"));
        return true;
    }

    @Override
    public boolean modelSetterMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
        //不生成getter
        return false;
    }

    @Override
    public boolean modelGetterMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
        //不生成setter
        return false;
    }

    @Override
    public boolean sqlMapGenerated(GeneratedXmlFile sqlMap, IntrospectedTable introspectedTable) {
        mapperName = introspectedTable.getFullyQualifiedTable().getDomainObjectName() + "Mapper";
        mapperExtendName = introspectedTable.getFullyQualifiedTable().getDomainObjectName() + "MapperExtend";
        daoServiceName = introspectedTable.getFullyQualifiedTable().getDomainObjectName() + "MapperExtend";
        sqlMap.setMergeable(false);
        return true;
    }

    @Override
    public List<GeneratedJavaFile> contextGenerateAdditionalJavaFiles(IntrospectedTable introspectedTable) {
        List<GeneratedJavaFile> res = new ArrayList<>();
        GeneratedJavaFile extendMapper = generateExtendMapper(introspectedTable);
        GeneratedJavaFile daoService = generateDaoService(introspectedTable);
        if (extendMapper != null) {
            res.add(extendMapper);
        }
        if (daoService != null) {
            res.add(daoService);
        }
        return res;
    }

    private GeneratedJavaFile generateExtendMapper(IntrospectedTable introspectedTable) {
        String filePath = genFilePath(baseDir, targetProject, targetPackage, mapperExtendName, ".java");

        if (!isOverWrite && new File(filePath).exists()) {
            //不重写
            System.out.println(filePath + "文件存在，不生成");
            return null;
        }

        //继承接口
        FullyQualifiedJavaType superClassType = new FullyQualifiedJavaType(mapperName);
        Interface unit = new Interface(targetPackage + "." + mapperExtendName);
        unit.addSuperInterface(superClassType);
        unit.setVisibility(JavaVisibility.PUBLIC);
        //import
        unit.addImportedType(new FullyQualifiedJavaType(introspectedTable.getMyBatis3JavaMapperType()));
        unit.addImportedType(new FullyQualifiedJavaType("org.apache.ibatis.annotations.Mapper"));
        //增加注解
        unit.addAnnotation("@Mapper");
        //增加注释
        unit.addJavaDocLine("/**");
        unit.addJavaDocLine(" * ");
        unit.addJavaDocLine(" * @author " + author);
        unit.addJavaDocLine(" * @date " + dateFormatter.format(new Date()));
        unit.addJavaDocLine(" */");

        return new GeneratedJavaFile(unit, targetProject, "utf-8", this.context.getJavaFormatter());
    }

    private GeneratedJavaFile generateDaoService(IntrospectedTable introspectedTable) {
        if (targetPackageService == null || targetPackageService.length() == 0) {
            return null;
        }
        String filePath = genFilePath(baseDir, targetProject, targetPackageService, daoServiceName, ".java");

        if (!isOverWrite && new File(filePath).exists()) {
            //不重写文件
            System.out.println(filePath + "文件存在，不生成");
            return null;
        }
        //类名
        TopLevelClass topLevelClass = new TopLevelClass(targetPackageService + "." + daoServiceName);
        //权限
        topLevelClass.setVisibility(JavaVisibility.PUBLIC);
        //import
        topLevelClass.addImportedType(introspectedTable.getBaseRecordType());
        topLevelClass.addImportedType(introspectedTable.getMyBatis3JavaMapperType());
        topLevelClass.addImportedType(introspectedTable.getMyBatis3JavaMapperType() + "Extend");
        topLevelClass.addImportedType("org.springframework.stereotype.Service");
        topLevelClass.addImportedType("javax.annotation.Resource");
        //类注解
        topLevelClass.addAnnotation("@Service");
        //属性
        Field field = new Field(lowerFirst(mapperName), new FullyQualifiedJavaType(mapperName));
        field.setVisibility(JavaVisibility.PRIVATE);
        field.addAnnotation("@Resource");
        topLevelClass.addField(field);

        field = new Field(lowerFirst(mapperExtendName), new FullyQualifiedJavaType(mapperExtendName));
        field.setVisibility(JavaVisibility.PRIVATE);
        field.addAnnotation("@Resource");
        topLevelClass.addField(field);
        //类注释
        topLevelClass.addJavaDocLine("/**");
        topLevelClass.addJavaDocLine(" * DaoService");
        topLevelClass.addJavaDocLine(" * @author " + author);
        topLevelClass.addJavaDocLine(" * @date " + dateFormatter.format(new Date()));
        topLevelClass.addJavaDocLine(" */");

        return new GeneratedJavaFile(topLevelClass, targetProject, "utf-8", this.context.getJavaFormatter());
    }


    @Override
    public List<GeneratedXmlFile> contextGenerateAdditionalXmlFiles(IntrospectedTable introspectedTable) {
        String filePath = genFilePath(baseDir, targetProjectXml, targetPackageXml, mapperExtendName, ".xml");
        List<String> oldFileContent = new ArrayList<>();
        String domainType = introspectedTable.getBaseRecordType();
        Document document = new Document(
                XmlConstants.MYBATIS3_MAPPER_CONFIG_PUBLIC_ID,
                XmlConstants.MYBATIS3_MAPPER_SYSTEM_ID);
        XmlElement root = new XmlElement("mapper");
        document.setRootElement(root);
        root.addAttribute(new Attribute("namespace", targetPackage + "." + mapperExtendName));
        root.addElement(new TextElement("<!--"));
        root.addElement(new TextElement("注释"));
        root.addElement(new TextElement("-->"));

        XmlElement resultMap = new XmlElement("resultMap");
        resultMap.addAttribute(new Attribute("id", "BaseResultMap"));
        resultMap.addAttribute(new Attribute("type", domainType));

        List<IntrospectedColumn> primaryKeyColumns = introspectedTable.getPrimaryKeyColumns();
        for (IntrospectedColumn primaryKeyColumn : primaryKeyColumns) {
            this.addResultElement(resultMap,
                    "id", primaryKeyColumn.getActualColumnName(), primaryKeyColumn.getJdbcTypeName(), primaryKeyColumn.getJavaProperty());
        }

        List<IntrospectedColumn> baseColumns = introspectedTable.getBaseColumns();
        for (IntrospectedColumn baseColumn : baseColumns) {
            this.addResultElement(resultMap, "result", baseColumn.getActualColumnName(), baseColumn.getJdbcTypeName(), baseColumn.getJavaProperty());
        }

        List<IntrospectedColumn> blobColumns = introspectedTable.getBLOBColumns();
        for (IntrospectedColumn blobColumn : blobColumns) {
            this.addResultElement(resultMap, "result", blobColumn.getActualColumnName(), blobColumn.getJdbcTypeName(), blobColumn.getJavaProperty());
        }
        root.addElement(resultMap);

        List<IntrospectedColumn> allColumns = introspectedTable.getAllColumns();
        StringBuilder builder = new StringBuilder();
        for (IntrospectedColumn column : allColumns) {
            builder.append(column.getActualColumnName()).append(", ");
        }
        String columnsText = builder.substring(0, builder.length() - 2);

        XmlElement baseColumnList = new XmlElement("sql");
        baseColumnList.addAttribute(new Attribute("id", "Base_Column_List"));
        baseColumnList.addElement(new TextElement(columnsText));
        root.addElement(baseColumnList);

        if (isOverWrite || !new File(filePath).exists()) {
            //如果需要重新或是新生成文件
            root.addElement(new TextElement("<!-- " + tagString + "-->"));
        } else {
            System.out.println(filePath + "文件存在，续写文件");
            oldFileContent = FileUtils.readFileToListByLine(filePath.toString());
            boolean flag = false;
            for (String str : oldFileContent) {
                if (str.startsWith("  ")) {
                    //删除前面的空格，对齐格式
                    str = str.substring(2);
                }
                if (!flag) {
                    if (str.contains(tagString)) {
                        flag = true;
                        root.addElement(new TextElement(str));
                    }
                    continue;
                }
                if (str.contains("</mapper>")) {
                    continue;
                }
                root.addElement(new TextElement(str));
            }
            if (!flag && oldFileContent.size() > 0) {
                //如果不存在标记注释，可能是用户删除了，重新增加
                root.addElement(new TextElement("<!-- " + tagString + "-->"));
            }
        }

        GeneratedXmlFile gxf = new GeneratedXmlFile(document,
                mapperExtendName + ".xml",
                targetPackageXml,
                targetProjectXml,
                false, context.getXmlFormatter());

        List<GeneratedXmlFile> res = new ArrayList<>(1);
        res.add(gxf);
        return res;
    }

    private void addResultElement(XmlElement resultMap, String ele, String actualColumnName, String jdbcTypeName, String javaProperty) {
        XmlElement result = new XmlElement(ele);
        result.addAttribute(new Attribute("column", actualColumnName));
        result.addAttribute(new Attribute("jdbcType", jdbcTypeName));
        result.addAttribute(new Attribute("property", javaProperty));
        resultMap.addElement(result);
    }

    private String genFilePath(String baseDir, String targetDir, String targetPath, String target, String suffix) {
        return baseDir + File.separator + targetDir +
                File.separator + targetPath.replaceAll("\\.", "/") + File.separator +
                target + suffix;
    }

    private String lowerFirst(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        char c = str.charAt(0);
        if (c <= 'Z' && c >= 'A') {
            c = (char) (c - 'A' + 'a');
        }
        return c + str.substring(1);
    }

}
