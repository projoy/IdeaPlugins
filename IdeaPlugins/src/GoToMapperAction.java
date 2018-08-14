import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.xml.XmlFile;

/**
 * @Describe: 根据DAO的方法名定位到mybatis 的mapper文件
 */
public class GoToMapperAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        System.out.println("Goto mapper!");

        PsiElement element = event.getData(PlatformDataKeys.PSI_ELEMENT);//鼠标所在的元素
        if(element == null){
            return;
        }

        String methodName = element.toString().replace("PsiMethod:","");

        //父类名
        PsiElement parentElement = element.getParent();
        if(parentElement == null){
            return;
        }
        PsiFile contaiFile = parentElement.getContainingFile();//所在的文件名
        String className = contaiFile.getName();//类名

        String mapperName;
        if(className.endsWith("DaoImpl.java")){
            mapperName = className.replace("DaoImpl.java","Mapper.xml");
        }else if(className.endsWith("Dao.java") ){
            mapperName = className.replace("Dao.java","Mapper.xml");
        }else{
            return;
        }

        //xml文件
        Project project = event.getProject();
        PsiFile[] files = PsiShortNamesCache.getInstance(project).getFilesByName(mapperName);

        if(files.length == 1){
            XmlFile xmlFile = (XmlFile)files[0];
            String strXml = xmlFile.getDocument().getText();

            if(StringUtil.isNotEmpty(strXml) && strXml.contains("id=\""+methodName+"\"")){
                gotoMapper(project,methodName,files[0].getVirtualFile(),strXml);
            }
        }

    }

    /**
     *
     * @param project 项目
     * @param methodName 方法名
     * @param mapperFile mapper文件
     * @param strXml mapper文件内容
     */
    private void gotoMapper(Project project, String methodName, VirtualFile mapperFile, String strXml){
        //打开文件
        OpenFileDescriptor fileDescriptor = new OpenFileDescriptor(project,mapperFile);
        Editor editor = FileEditorManager.getInstance(project).openTextEditor(fileDescriptor,true);
        //查找方法在文件中的位置
        String[] split = strXml.split("\n");
        int lineNumber = 0;
        for (int i = 0; i < split.length; i++) {
            String line = split[i];
            if (StringUtil.isNotEmpty(line) && line.contains(methodName)) {
                lineNumber = i;
                break;
            }
        }

        //打开坐标系统
        CaretModel caretModel = editor.getCaretModel();
        LogicalPosition logicalPosition = caretModel.getLogicalPosition();
        logicalPosition.leanForward(true);
        //定位方法所在的位置
        LogicalPosition logical = new LogicalPosition(lineNumber,logicalPosition.column);
        //跳转到方法位置
        caretModel.moveToLogicalPosition(logical);
        SelectionModel selectionModel = editor.getSelectionModel();
        selectionModel.selectLineAtCaret();

    }
}
