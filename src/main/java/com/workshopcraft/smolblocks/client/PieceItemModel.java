package com.workshopcraft.smolblocks.client;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.workshopcraft.smolblocks.content.Cells;
import com.workshopcraft.smolblocks.content.Shape;
import com.workshopcraft.smolblocks.content.SmolContent;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Item model for pieces: the source block's quads cut to the piece shape, centred, with block display transforms. */
public class PieceItemModel implements ItemModel {
    private record Key(BlockState state, Shape shape) {}

    private record Built(List<BakedQuad> quads, Vector3fc[] extents, int maxTint) {}

    private final ModelRenderProperties properties;
    private final Matrix4fc transformation;
    private final Map<Key, Built> cache = new ConcurrentHashMap<>();

    public PieceItemModel(ModelRenderProperties properties, Matrix4fc transformation) {
        this.properties = properties;
        this.transformation = transformation;
    }

    private static Built build(Key key) {
        BlockState[] cells = new BlockState[8];
        int mask = key.shape().canonicalMask();
        for (int i = 0; i < 8; i++) if ((mask & 1 << i) != 0) cells[i] = key.state();
        Shape s = key.shape();
        Vector3f offset = new Vector3f(s.spanX ? 0 : 0.25F, s.spanY ? 0 : 0.25F, s.spanZ ? 0 : 0.25F);
        QuadCollection quads = MicroQuads.build(cells, false, false, offset);
        int maxTint = -1;
        for (BakedQuad q : quads.getAll()) maxTint = Math.max(maxTint, q.materialInfo().tintIndex());
        return new Built(quads.getAll(), CuboidItemModelWrapper.computeExtents(quads.getAll()), maxTint);
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver, ItemDisplayContext displayContext,
                       @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        BlockState state = item.get(SmolContent.SOURCE);
        Shape shape = item.get(SmolContent.SHAPE);
        if (state == null || shape == null) return;
        Key key = new Key(state, shape);
        Built built = cache.computeIfAbsent(key, PieceItemModel::build);
        output.appendModelIdentityElement(this);
        output.appendModelIdentityElement(key);
        ItemStackRenderState.LayerRenderState layer = output.newLayer();
        properties.applyToLayer(layer, displayContext);
        layer.setParticleMaterial(MicroQuads.models().getParticleMaterial(state));
        layer.setLocalTransform(transformation);
        layer.setExtents(() -> built.extents());
        if (built.maxTint() >= 0) {
            IntList tints = layer.tintLayers();
            for (int t = 0; t <= built.maxTint(); t++) {
                BlockTintSource source = Minecraft.getInstance().getBlockColors().getTintSource(state, t);
                tints.add(source == null ? -1 : source.color(state));
            }
        }
        layer.prepareQuadList().addAll(built.quads());
    }

    public record Unbaked(Identifier base) implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.optionalFieldOf("base", Identifier.withDefaultNamespace("block/stone")).forGetter(Unbaked::base)
        ).apply(i, Unbaked::new));

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(base);
        }

        @Override
        public ItemModel bake(BakingContext context, Matrix4fc transformation) {
            ModelBaker baker = context.blockModelBaker();
            ResolvedModel model = baker.getModel(base);
            TextureSlots slots = model.getTopTextureSlots();
            return new PieceItemModel(ModelRenderProperties.fromResolvedModel(baker, model, slots), new Matrix4f(transformation));
        }
    }
}
