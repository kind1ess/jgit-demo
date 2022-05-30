package top.kindless.jgit;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.io.FileUtil;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class JgitDemo {

    public static void main(String[] args) throws IOException {
        String gitRepositoryPath = ""; // git仓库中.git文件夹路径
        String insertSrcFilePath = ""; // 要向git仓库中插入文件的路径，如果是文件则调用insertFile，是目录则调用insertDir
        Repository repository = new RepositoryBuilder().setGitDir(new File(gitRepositoryPath)).build();
        LocalDateTime now = LocalDateTime.now();
        AnyObjectId objectId = insertDir(new File(insertSrcFilePath), repository);
        System.out.println("向git插入文件夹耗时" + LocalDateTimeUtil.between(now, LocalDateTimeUtil.now(), ChronoUnit.MILLIS) + "毫秒");
        now = LocalDateTime.now();
        checkoutDir(new File("script_temp"), repository, objectId);
        System.out.println("从git检出文件夹耗时" + LocalDateTimeUtil.between(now, LocalDateTimeUtil.now(), ChronoUnit.MILLIS) + "毫秒");
    }

    public static AnyObjectId insertDir(File srcFile, Repository repository) throws IOException {
        if (!srcFile.isDirectory()) {
            throw new RuntimeException("不是目录");
        }
        TreeFormatter treeFormatter = new TreeFormatter();
        File[] files = srcFile.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    treeFormatter.append(file.getName(), FileMode.TREE, insertDir(file, repository));
                } else {
                    treeFormatter.append(file.getName(), FileMode.REGULAR_FILE, insertFile(file, repository));
                }
            }
        }
        return repository.getObjectDatabase().newInserter().insert(treeFormatter);
    }

    public static AnyObjectId insertFile(File srcFile, Repository repository) throws IOException {
        if (!srcFile.isFile()) {
            throw new RuntimeException("不是文件");
        }
        byte[] bytes = FileUtil.readBytes(srcFile);
        return repository.getObjectDatabase().newInserter().insert(Constants.OBJ_BLOB, bytes, 0, bytes.length);
    }

    public static void checkoutDir(File destDir, Repository repository, AnyObjectId objectId) throws IOException {
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(objectId);
        while (treeWalk.next()) {
            String pathString = treeWalk.getPathString();
            if (treeWalk.isSubtree()) {
                new File(destDir, pathString).mkdirs();
                treeWalk.enterSubtree();
            } else {
                checkoutFile(new File(destDir, pathString), repository, treeWalk.getObjectId(0));
            }
        }
    }

    public static void checkoutFile(File destFile, Repository repository, AnyObjectId objectId) throws IOException {
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }
        FileUtil.writeFromStream(repository.getObjectDatabase().open(objectId).openStream(), destFile);
    }

}
