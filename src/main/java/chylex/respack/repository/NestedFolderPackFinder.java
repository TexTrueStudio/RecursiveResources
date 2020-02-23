package chylex.respack.repository;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.FilePack;
import net.minecraft.resources.FolderPack;
import net.minecraft.resources.IPackFinder;
import net.minecraft.resources.ResourcePackInfo;
import net.minecraft.resources.ResourcePackInfo.IFactory;
import net.minecraft.resources.ResourcePackInfo.Priority;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import java.io.File;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class NestedFolderPackFinder implements IPackFinder{
	public static void register(){
		Minecraft mc = Minecraft.getInstance();
		mc.getResourcePackList().addPackFinder(new NestedFolderPackFinder(mc.getFileResourcePacks()));
	}
	
	private static final File[] EMPTY_FILE_ARRAY = new File[0];
	
	private static File[] wrap(File[] filesOrNull){
		return filesOrNull == null ? EMPTY_FILE_ARRAY : filesOrNull;
	}
	
	private final File root;
	private final int rootLength;
	
	private NestedFolderPackFinder(File root){
		this.root = root;
		this.rootLength = root.getAbsolutePath().length();
	}
	
	@Override
	public <T extends ResourcePackInfo> void addPackInfosToMap(Map<String, T> nameToPackMap, IFactory<T> packInfoFactory){
		File[] folders = root.listFiles(File::isDirectory);
		
		for(File folder : wrap(folders)){
			if (!isFolderPack(folder)){
				processFolder(folder, nameToPackMap, packInfoFactory);
			}
		}
	}
	
	private boolean isFolderPack(File folder){
		return new File(folder, "pack.mcmeta").exists();
	}
	
	private <T extends ResourcePackInfo> void processFolder(File folder, Map<String, T> nameToPackMap, IFactory<T> packInfoFactory){
		if (isFolderPack(folder)){
			addPack(folder, nameToPackMap, packInfoFactory);
			return;
		}
		
		File[] zipFiles = folder.listFiles(file -> file.isFile() && file.getName().endsWith(".zip"));
		
		for(File zipFile : wrap(zipFiles)){
			addPack(zipFile, nameToPackMap, packInfoFactory);
		}
		
		File[] nestedFolders = folder.listFiles(File::isDirectory);
		
		for(File nestedFolder : wrap(nestedFolders)){
			processFolder(nestedFolder, nameToPackMap, packInfoFactory);
		}
	}
	
	private <T extends ResourcePackInfo> void addPack(File fileOrFolder, Map<String, T> nameToPackMap, IFactory<T> packInfoFactory){
		String name = "file/" + StringUtils.removeStart(fileOrFolder.getAbsolutePath().substring(rootLength).replace('\\', '/'), "/");
		T info;
		
		if (fileOrFolder.isDirectory()){
			info = ResourcePackInfo.createResourcePack(name, false, () -> new FolderPack(fileOrFolder), packInfoFactory, Priority.TOP);
		}
		else{
			info = ResourcePackInfo.createResourcePack(name, false, () -> new FilePack(fileOrFolder), packInfoFactory, Priority.TOP);
		}
		
		if (info != null){
			nameToPackMap.put(name, info);
		}
	}
}