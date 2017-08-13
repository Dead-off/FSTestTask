package maxim.z;

public interface IFile {

    IFile child(String name);

    IFile parent();

    String getPath();

    String[] parseFileNames();

}
