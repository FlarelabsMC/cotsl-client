package com.flarelabsmc.cotsl.launch.transform;

import com.flarelabsmc.cotsl.launch.Launcher;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CModifyExpressionValue;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.loading.moddiscovery.*;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;

import java.io.IOException;
import java.util.List;

@CTransformer(ModDiscoverer.class)
public class ModDiscovererTransformer {
    @CModifyExpressionValue(
            method = "discoverMods",
            target = @CTarget(
                    value = "INVOKE",
                    ordinal = 2,
                    target = "Lnet/neoforged/fml/loading/UniqueModListBuilder$UniqueModListData;modFiles()Ljava/util/List;"
            )
    )
    public List<ModFile> discoverMods(List<ModFile> mods) throws IOException {
        JarContents contents = JarContents.ofPath(Launcher.findSelf().toPath());
        ModJarMetadata metadata = new ModJarMetadata();
        ModFile modFile = new ModFile(
                contents,
                metadata,
                ModFileParser::modsTomlParser,
                ModFileDiscoveryAttributes.DEFAULT
        );
        metadata.setModFile(modFile);
        mods.add(modFile);
        return mods;
    }
}
