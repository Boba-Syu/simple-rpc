package cn.bobasyu.core.spi;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义SPI组件类
 */
public class ExtensionLoader {
    public static String EXTENSION_LOADER_DIR_PREFIX = "META-INF/irpc";

    public static Map<String, LinkedHashMap<String, Class<?>>> EXTENSION_LOADER_CLASS_CACHE = new ConcurrentHashMap<>();

    public void loadExtension(Class<?> clazz) throws Exception {
        if (clazz == null) {
            throw new IllegalArgumentException("class in null");
        }
        String spiFilePath = EXTENSION_LOADER_DIR_PREFIX + clazz.getName();
        ClassLoader classLoader = this.getClass().getClassLoader();
        Enumeration<URL> enumeration = classLoader.getResources(spiFilePath);
        while (enumeration.hasMoreElements()) {
            URL url = enumeration.nextElement();
            InputStreamReader inputStreamReader = new InputStreamReader(url.openStream());
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line = null;
            LinkedHashMap<String, Class<?>> classMap = new LinkedHashMap<>();
            while ((line = bufferedReader.readLine()) != null) {
                // 如果配置中加入了#开头则表示忽略该配置
                if (line.startsWith("#")) {
                    continue;
                }
                String[] lineArr = line.split("=");
                String implClassName = lineArr[0];
                String interfaceName = lineArr[1];
                classMap.put(implClassName, Class.forName(interfaceName));
            }
            // 触发class文件的加载
            if (EXTENSION_LOADER_CLASS_CACHE.containsKey(clazz.getName())) {
                // 支持开发者自定义配置
                EXTENSION_LOADER_CLASS_CACHE.get(clazz.getName()).putAll(classMap);
            } else {
                EXTENSION_LOADER_CLASS_CACHE.put(clazz.getName(), classMap);
            }
        }
    }
}
