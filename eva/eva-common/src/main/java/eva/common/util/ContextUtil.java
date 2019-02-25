package eva.common.util;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.Sets;

public class ContextUtil {
	
	public static String BASE_DIR = null;
	
	public static final String CLASS_SUFFIX = ".class";
	
	static {
		URL url = ContextUtil.class.getClassLoader().getResource("");
		String file = url.getFile();
		if (file.startsWith("/")) {
			BASE_DIR = file.substring(1);
		}
	}
	
	public static Set<Class<?>> scanPackage(String...packages) throws ClassNotFoundException {
		Set<Class<?>> collection = Sets.newConcurrentHashSet();
		FileFilter[] filters = {
			new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.isFile() && pathname.getName().toLowerCase().endsWith(CLASS_SUFFIX);
				}
			}
		};
		if (Objects.isNull(packages)) {
			collectClassInstances(BASE_DIR, collection, filters);
		} else {
			for (String path: packages) {
				collectClassInstances(BASE_DIR + "/" + (path.replace(".", "/")), collection, filters);
			}
		}
		return collection;
	}
	
	private static void collectClassInstances(String dir, Collection<Class<?>> collection, FileFilter...filters) throws ClassNotFoundException {
		File path = new File(dir);
		if (path.exists() && path.isDirectory()) {
			File[] files = path.listFiles();
			if (Objects.nonNull(files) && files.length > 0) {
				String newPath = null;
				for (File unknown: files) {
					if (unknown.isDirectory()) {
						newPath = unknown.getAbsolutePath();
						collectClassInstances(newPath, collection, filters);
					} else {
						Function<FileFilter[], Boolean> func = new Function<FileFilter[], Boolean>() {
							@Override
							public Boolean apply(FileFilter[] t) {
								for (FileFilter f: t) {
									if (!f.accept(unknown)) {
										return false;
									}
								}
								return true;
							}
						};
						if (func.apply(filters)) {
							String className = unknown.getPath().replace("\\", "/").replace(BASE_DIR, "").replace("/", ".").replace(CLASS_SUFFIX, "");
							Class<?> clazz = Class.forName(className);
							collection.add(clazz);
						}
					}
				}
			}
		}
	}
	
}
