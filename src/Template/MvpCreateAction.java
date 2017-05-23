package Template;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by zhangxun on 2016/12/6.
 */
public class MvpCreateAction extends AnAction {

    private Project project;
    //包名
    private String packageName = "";
    private String mAuthor;//作者
    private String mModuleName;//模块名称
    private boolean mIsActivity;//是否是 act,or fragment
    private VirtualFile virtualFile;
    private String currentDirPath;//当前所在目录路径

    private enum CodeType {
        Activity, Fragment, Contract, Presenter, BaseView, BasePresenter, MvpBaseActivity, MvpBaseFragment
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        project = e.getProject();
        virtualFile = DataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        currentDirPath = virtualFile.getPath();
        packageName = getPackageName();
        if (currentDirPath == null || currentDirPath.length() == 0) {
            Messages.showErrorDialog(project, "请在文件浏览器选择某个目录,再启动插件", "发送了一个不可描述的错误~");
            return;
        }
        init();
        refreshProject(e);
    }

    /**
     * 刷新项目
     *
     * @param e
     */
    private void refreshProject(AnActionEvent e) {
        e.getProject().getBaseDir().refresh(false, true);
    }

    /**
     * 初始化Dialog
     */
    private void init() {
        MyDialog myDialog = new MyDialog(new MyDialog.DialogCallBack() {
            @Override
            public void ok(String author, String moduleName, boolean isActivity) {
                mAuthor = author;
                mModuleName = moduleName;
                if (moduleName == null || moduleName.length() == 0) {
                    Messages.showErrorDialog(project, "卧槽,moduleName 不能为空!为空了还搞毛!", "发送了一个不可描述的错误~");
                }
                if (mModuleName.length() == 1) {
                    mModuleName = mModuleName.toUpperCase();
                } else {
                    mModuleName = mModuleName.substring(0, 1).toUpperCase()
                            + mModuleName.substring(1).toLowerCase();
                }
                mIsActivity = isActivity;
                createClassFiles();
            }
        });
        myDialog.setVisible(true);

    }

    /**
     * 生成类文件
     */
    private void createClassFiles() {
        createClassFile(CodeType.Contract);
        createClassFile(CodeType.Presenter);
        if (mIsActivity) {
            createClassFile(CodeType.Activity);
        } else {
            createClassFile(CodeType.Fragment);
        }
//        createBaseClassFile(CodeType.BaseView);
//        createBaseClassFile(CodeType.BasePresenter);
//        createBaseClassFile(CodeType.MvpBaseActivity);
//        createBaseClassFile(CodeType.MvpBaseFragment);
    }

    /**
     * 生成base类
     *
     * @param codeType
     */
    private void createBaseClassFile(CodeType codeType) {
        String fileName = "";
        String content = "";
        String basePath = getAppPath() + "base/";
        switch (codeType) {
            case BaseView:
                if (!new File(basePath + "BaseView.java").exists()) {
                    fileName = "TemplateBaseView.txt";
                    content = ReadTemplateFile(fileName);
                    content = dealTemplateContent(content);
                    writeToFile(content, basePath, "BaseView.java");
                }
                break;
            case BasePresenter:
                if (!new File(basePath + "BasePresenter.java").exists()) {
                    fileName = "TemplateBasePresenter.txt";
                    content = ReadTemplateFile(fileName);
                    content = dealTemplateContent(content);
                    writeToFile(content, basePath, "BasePresenter.java");
                }
                break;
            case MvpBaseActivity:
                if (!new File(basePath + "MvpBaseActivity.java").exists()) {
                    fileName = "TemplateMvpBaseActivity.txt";
                    content = ReadTemplateFile(fileName);
                    content = dealTemplateContent(content);
                    writeToFile(content, basePath, "MvpBaseActivity.java");
                }
                break;
            case MvpBaseFragment:
                if (!new File(basePath + "MvpBaseFragment.java").exists()) {
                    fileName = "TemplateMvpBaseFragment.txt";
                    content = ReadTemplateFile(fileName);
                    content = dealTemplateContent(content);
                    writeToFile(content, basePath, "MvpBaseFragment.java");
                }
                break;
        }
    }

    /**
     * 生成mvp框架代码
     *
     * @param codeType
     */
    private void createClassFile(CodeType codeType) {
        String fileName = "";
        String content = "";
        String appPath = getAppPath();
        switch (codeType) {
            case Activity:
                fileName = "TemplateActivity.txt";
                content = ReadTemplateFile(fileName);
                content = dealTemplateContent(content);
                writeToFile(content, appPath + mModuleName.toLowerCase(), mModuleName + "Activity.java");
                break;
            case Fragment:
                fileName = "TemplateFragment.txt";
                content = ReadTemplateFile(fileName);
                content = dealTemplateContent(content);
                writeToFile(content, appPath + mModuleName.toLowerCase(), mModuleName + "Fragment.java");
                break;
            case Contract:
                fileName = "TemplateContract.txt";
                content = ReadTemplateFile(fileName);
                content = dealTemplateContent(content);
                writeToFile(content, appPath + mModuleName.toLowerCase(), mModuleName + "Contract.java");
                break;
            case Presenter:
                fileName = "TemplatePresenter.txt";
                content = ReadTemplateFile(fileName);
                content = dealTemplateContent(content);
                writeToFile(content, appPath + mModuleName.toLowerCase(), mModuleName + "Presenter.java");
                break;
        }
    }

    /**
     * 获取包名文件路径
     *
     * @return
     */
    private String getAppPath() {
//        String packagePath = packageName.replace(".", "/");
//        String appPath = project.getBasePath() + "/App/src/main/java/" + packagePath + "/";
//        return appPath;
        return currentDirPath + "/";
    }

    /**
     * 替换模板中字符
     *
     * @param content
     * @return
     */
    private String dealTemplateContent(String content) {
        content = content.replace("$name", mModuleName);
        if (content.contains("$packagename")) {
            content = content.replace("$packagename", getCurrentPackage() + "." + mModuleName.toLowerCase());
        }
        if (content.contains("$basepackagename")) {
            content = content.replace("$basepackagename", getCurrentPackage() + ".base");
        }
        if (content.contains("$description")) {
            content = content.replace("$description",
                    mModuleName + " mvp module , generate by plugin");
        }
        content = content.replace("$author", mAuthor);
        content = content.replace("$date", getDate());
        return content;
    }

    //当前目录的包名
    private String getCurrentPackage() {
        return currentDirPath.substring(currentDirPath.indexOf("java") + 5,
                currentDirPath.length()).replace("/", ".");
    }

    /**
     * 获取当前时间
     *
     * @return
     */
    public String getDate() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        String dateString = formatter.format(currentTime);
        return dateString;
    }


    /**
     * 读取模板文件中的字符内容
     *
     * @param fileName 模板文件名
     * @return
     */
    private String ReadTemplateFile(String fileName) {
        InputStream in = null;
        in = this.getClass().getResourceAsStream("/Template/" + fileName);
        String content = "";
        try {
            content = new String(readStream(in));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }


    private byte[] readStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = -1;
        try {
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            outputStream.close();
            inputStream.close();
        }

        return outputStream.toByteArray();
    }


    /**
     * 生成
     *
     * @param content   类中的内容
     * @param classPath 类文件路径
     * @param className 类文件名称
     */
    private void writeToFile(String content, String classPath, String className) {
        try {
            File floder = new File(classPath);
            if (!floder.exists()) {
                floder.mkdirs();
            }

            File file = new File(classPath + "/" + className);
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 从AndroidManifest.xml文件中获取当前app的包名
     *
     * @return
     */
    private String getPackageName() {
        String package_name = "";
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(project.getBasePath() + "/App/src/main/AndroidManifest.xml");

            NodeList nodeList = doc.getElementsByTagName("manifest");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                Element element = (Element) node;
                package_name = element.getAttribute("package");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return package_name;
    }
}
