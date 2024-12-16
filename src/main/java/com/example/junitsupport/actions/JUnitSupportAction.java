package com.example.junitsupport.actions;

import com.example.junitsupport.services.RuleParser;
import com.example.junitsupport.services.TestCodeRefactorer;
import com.example.junitsupport.services.TestAnalysisResult;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;

public class JUnitSupportAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        VirtualFile vf = e.getDataContext().getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE);
        if (vf == null) return;

        PsiFile psiFile = PsiManager.getInstance(project).findFile(vf);
        if (psiFile == null) return;

        // rules.md読み込み
        RuleParser ruleParser = new RuleParser(Paths.get(project.getBasePath(), "rules.md").toString());

        // リファクタリング実行
        TestCodeRefactorer refactorer = new TestCodeRefactorer(ruleParser, project);
        TestAnalysisResult result = refactorer.analyzeAndRefactor(psiFile);

        // 結果に応じてユーザーに通知したり、Event Logにメッセージを出したりできる
        // 例: 
        // Notifications.Bus.notify(new Notification("JunitSupport", "JUnit Support", "Refactor completed with X TODO comments added", NotificationType.INFORMATION));
    }

    // 下はjunit用メソッド、実際では使用していない
    public boolean testTrue(){
        return true;
    }

    public boolean testFalse(){
        return true;
    }

    public boolean testDuplicationin(int a){
        return true;
    }

    public boolean testDuplicationin(String a){
        return true;
    }

    public int abc ;
    public boolean testReturn(){
        abc = 123;
        return true;
    }
}
