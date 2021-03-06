package thebombzen.mods.autoswitch.configuration;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import thebombzen.mods.thebombzenapi.ThebombzenAPI;
import thebombzen.mods.thebombzenapi.configuration.CompoundExpression;
import thebombzen.mods.thebombzenapi.configuration.ConfigFormatException;

public class BlockItemIdentifier extends CompoundExpression<SingleValueIdentifier> {

	public static BlockItemIdentifier parseBlockItemIdentifier(String info) throws ConfigFormatException {
		
		if (info.length() == 0){
			throw new ConfigFormatException("Error parsing identifier!");
		}
		
		if (!info.contains("&") && !info.contains("|") && !info.contains("^") && !info.contains("!")){
			if (info.startsWith("(") && info.endsWith(")")){
				return parseBlockItemIdentifier(info.substring(1, info.length() - 1));
			}
			return new BlockItemIdentifier(SingleBlockItemIdentifier.parseSingleBlockItemIdentifier(info));
		}
		
		int index = info.indexOf('^');
		while (index >= 0){
			if (ThebombzenAPI.isSeparatorAtTopLevel(info, index)){
				String before = info.substring(0, index);
				String after = info.substring(index + 1);
				return new BlockItemIdentifier(XOR, BlockItemIdentifier.parseBlockItemIdentifier(before), BlockItemIdentifier.parseBlockItemIdentifier(after));
			} else {
				index = info.indexOf('^', index + 1);
			}
		}
		
		index = info.indexOf('|');
		while (index >= 0){
			if (ThebombzenAPI.isSeparatorAtTopLevel(info, index)){
				String before = info.substring(0, index);
				String after = info.substring(index + 1);
				return new BlockItemIdentifier(OR, BlockItemIdentifier.parseBlockItemIdentifier(before), BlockItemIdentifier.parseBlockItemIdentifier(after));
			} else {
				index = info.indexOf('|', index + 1);
			}
		}
		
		index = info.indexOf('&');
		while (index >= 0){
			if (ThebombzenAPI.isSeparatorAtTopLevel(info, index)){
				String before = info.substring(0, index);
				String after = info.substring(index + 1);
				return new BlockItemIdentifier(AND, BlockItemIdentifier.parseBlockItemIdentifier(before), BlockItemIdentifier.parseBlockItemIdentifier(after));
			} else {
				index = info.indexOf('&', index + 1);
			}
		}
		
		if (info.startsWith("!")){
			return new BlockItemIdentifier(NOT, BlockItemIdentifier.parseBlockItemIdentifier(info.substring(1)), null);
		}
		
		if (info.startsWith("(") && info.endsWith(")")){
			return BlockItemIdentifier.parseBlockItemIdentifier(info.substring(1, info.length() - 1));
		}
		
		throw new ConfigFormatException("Error parsing identifier!");
		
	}
	
	public BlockItemIdentifier(int type, BlockItemIdentifier first, BlockItemIdentifier second){
		super(type, first, second);
	}

	public BlockItemIdentifier(SingleBlockItemIdentifier singleID){
		super(singleID);
	}

	public boolean contains(Block block, int metadata){
		return contains(new SingleValueIdentifier(block, metadata));
	}
	
	public boolean contains(ItemStack itemStack){
		if (itemStack == null){
			return false;
		}
		return contains(new SingleValueIdentifier(itemStack));
	}
	
}
