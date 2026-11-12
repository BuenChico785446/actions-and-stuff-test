[Incubating] Problems report is available at: file:///F:/ZZZZZZZZZZZZZZZZZZZZaLL%20code%20things/GITHUB/actions-and-stuff-test/BedrockMotion/BedrockMotion/build/reports/problems/problems-report.html

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':compileJava' (registered by plugin class 'org.gradle.api.plugins.JavaBasePlugin').
> Compilation failed; see the compiler output below.
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\ModMenuIntegration.java:10: Fehler: Symbol nicht gefunden
      public ConfigScreenFactory<?> getModConfigScreenFactory() {
             ^
    Symbol: Klasse ConfigScreenFactory
    Ort: Klasse ModMenuIntegration
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\runtime\BmConfig.java:13: Fehler: Symbol nicht gefunden
      private static volatile ConfigHolder<BedrockMotionConfig> holder;
                              ^
    Symbol: Klasse ConfigHolder
    Ort: Klasse BmConfig
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\BmConfigScreen.java:35: Fehler: Symbol nicht gefunden
          toggles.add(new Toggle("Bedrock packs (needs reload)", () -> BmConfig.enabled, v -> BmConfig.enabled = v));
                                                                               ^
    Symbol: Variable enabled
    Ort: Klasse BmConfig
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\BmConfigScreen.java:35: Fehler: Symbol nicht gefunden
          toggles.add(new Toggle("Bedrock packs (needs reload)", () -> BmConfig.enabled, v -> BmConfig.enabled = v));
                                                                                                      ^
    Symbol: Variable enabled
    Ort: Klasse BmConfig
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\BmConfigScreen.java:37: Fehler: Symbol nicht gefunden
          for (String name : BmConfig.FEATURES) {
                                     ^
    Symbol: Variable FEATURES
    Ort: Klasse BmConfig
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\ModMenuIntegration.java:11: Fehler: Symbol nicht gefunden
          return parent -> AutoConfigClient.getConfigScreen(BedrockMotionConfig.class, parent).get();
                           ^
    Symbol: Variable AutoConfigClient
    Ort: Klasse ModMenuIntegration
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\runtime\BmConfig.java:19: Fehler: Symbol nicht gefunden
          ConfigHolder<BedrockMotionConfig> registered = AutoConfig.register(BedrockMotionConfig.class, GsonConfigSerializer::new);
          ^
    Symbol: Klasse ConfigHolder
    Ort: Klasse BmConfig
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\runtime\BmConfig.java:19: Fehler: Symbol nicht gefunden
          ConfigHolder<BedrockMotionConfig> registered = AutoConfig.register(BedrockMotionConfig.class, GsonConfigSerializer::new);
                                                         ^
    Symbol: Variable AutoConfig
    Ort: Klasse BmConfig
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\runtime\BmConfig.java:19: Fehler: Symbol nicht gefunden
          ConfigHolder<BedrockMotionConfig> registered = AutoConfig.register(BedrockMotionConfig.class, GsonConfigSerializer::new);
                                                                                                        ^
    Symbol: Klasse GsonConfigSerializer
    Ort: Klasse BmConfig
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\runtime\BmConfig.java:33: Fehler: Symbol nicht gefunden
          ConfigHolder<BedrockMotionConfig> current = holder;
          ^
    Symbol: Klasse ConfigHolder
    Ort: Klasse BmConfig
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:69: Fehler: Methode ├╝berschreibt oder implementiert keine Methode aus einem Supertyp
      @Override
      ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\ModMenuIntegration.java:9: Fehler: Methode ├╝berschreibt oder implementiert keine Methode aus einem Supertyp
      @Override
      ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:15: Fehler: Symbol nicht gefunden
  public class BedrockMotionConfig implements ConfigData {
                                              ^
    Symbol: Klasse ConfigData
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:14: Fehler: Symbol nicht gefunden
  @Config(name = BedrockMotionClient.MOD_ID)
   ^
    Symbol: Klasse Config
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\ModMenuIntegration.java:7: Fehler: Symbol nicht gefunden
  public final class ModMenuIntegration implements ModMenuApi {
                                                   ^
    Symbol: Klasse ModMenuApi
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\BmConfigScreen.java:63: Fehler: Symbol nicht gefunden
              BmConfig.save();
                      ^
    Symbol: Methode save()
    Ort: Klasse BmConfig
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\BmConfigScreen.java:72: Fehler: Symbol nicht gefunden
          BmConfig.save();
                  ^
    Symbol: Methode save()
    Ort: Klasse BmConfig
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:10: Fehler: Package me.shedaniel.autoconfig ist nicht vorhanden
  import me.shedaniel.autoconfig.ConfigData;
                                ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:11: Fehler: Package me.shedaniel.autoconfig.annotation ist nicht vorhanden
  import me.shedaniel.autoconfig.annotation.Config;
                                           ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:12: Fehler: Package me.shedaniel.autoconfig.annotation ist nicht vorhanden
  import me.shedaniel.autoconfig.annotation.ConfigEntry;
                                           ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\ModMenuIntegration.java:3: Fehler: Package com.terraformersmc.modmenu.api ist nicht vorhanden
  import com.terraformersmc.modmenu.api.ConfigScreenFactory;
                                       ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\ModMenuIntegration.java:4: Fehler: Package com.terraformersmc.modmenu.api ist nicht vorhanden
  import com.terraformersmc.modmenu.api.ModMenuApi;
                                       ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\ModMenuIntegration.java:5: Fehler: Package me.shedaniel.autoconfig ist nicht vorhanden
  import me.shedaniel.autoconfig.AutoConfigClient;
                                ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\runtime\BmConfig.java:5: Fehler: Package me.shedaniel.autoconfig ist nicht vorhanden
  import me.shedaniel.autoconfig.AutoConfig;
                                ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\runtime\BmConfig.java:6: Fehler: Package me.shedaniel.autoconfig ist nicht vorhanden
  import me.shedaniel.autoconfig.ConfigHolder;
                                ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\runtime\BmConfig.java:7: Fehler: Package me.shedaniel.autoconfig.serializer ist nicht vorhanden
  import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
                                           ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:20: Fehler: Package ConfigEntry ist nicht vorhanden
      @ConfigEntry.Category("general")
                  ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:21: Fehler: Package ConfigEntry.Gui ist nicht vorhanden
      @ConfigEntry.Gui.Tooltip
                      ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:24: Fehler: Package ConfigEntry ist nicht vorhanden
      @ConfigEntry.Category("general")
                  ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:25: Fehler: Package ConfigEntry.Gui ist nicht vorhanden
      @ConfigEntry.Gui.Tooltip
                      ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:28: Fehler: Package ConfigEntry ist nicht vorhanden
      @ConfigEntry.Category("general")
                  ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:29: Fehler: Package ConfigEntry.Gui ist nicht vorhanden
      @ConfigEntry.Gui.Tooltip
                      ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:32: Fehler: Package ConfigEntry ist nicht vorhanden
      @ConfigEntry.Category("general")
                  ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:33: Fehler: Package ConfigEntry.Gui ist nicht vorhanden
      @ConfigEntry.Gui.Tooltip
                      ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:36: Fehler: Package ConfigEntry ist nicht vorhanden
      @ConfigEntry.Category("features")
                  ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:37: Fehler: Package ConfigEntry.Gui ist nicht vorhanden
      @ConfigEntry.Gui.TransitiveObject
                      ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:40: Fehler: Package ConfigEntry.Gui ist nicht vorhanden
      @ConfigEntry.Gui.Excluded
                      ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:43: Fehler: Package ConfigEntry.Gui ist nicht vorhanden
      @ConfigEntry.Gui.Excluded
                      ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:47: Fehler: Package ConfigEntry.Gui ist nicht vorhanden
          @ConfigEntry.Gui.Tooltip
                          ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:50: Fehler: Package ConfigEntry.Gui ist nicht vorhanden
          @ConfigEntry.Gui.Tooltip
                          ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:53: Fehler: Package ConfigEntry.Gui ist nicht vorhanden
          @ConfigEntry.Gui.Tooltip
                          ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:56: Fehler: Package ConfigEntry.Gui ist nicht vorhanden
          @ConfigEntry.Gui.Tooltip
                          ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:59: Fehler: Package ConfigEntry.Gui ist nicht vorhanden
          @ConfigEntry.Gui.Tooltip
                          ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:62: Fehler: Package ConfigEntry.Gui ist nicht vorhanden
          @ConfigEntry.Gui.Tooltip
                          ^
  F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion\src\main\java\dev\bedrockmotion\client\config\BedrockMotionConfig.java:65: Fehler: Package ConfigEntry.Gui ist nicht vorhanden
          @ConfigEntry.Gui.Tooltip
                          ^
  45 Fehler

* Try:
> Check your code and dependencies to fix the compilation error(s)
> Run with --scan to get full insights from a Build Scan (powered by Develocity).

BUILD FAILED in 25s
1 actionable task: 1 executed
PS F:\ZZZZZZZZZZZZZZZZZZZZaLL code things\GITHUB\actions-and-stuff-test\BedrockMotion\BedrockMotion> 