package com.mindoo.domino.jna.errors.errortexts;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.mindoo.domino.jna.errors.IErrorTextProvider;

/**
 * Default implementation of {@link IErrorTextProvider} that loads
 * its error texts from method annotations.
 * 
 * @author Karsten Lehmann
 */
public class DefaultErrorTextProvider implements IErrorTextProvider {

	@Override
	public int getPriority() {
		return 50;
	}

	@Override
	public Map<Short, String> getErrorTexts() {
		Map<Short, String> texts = new HashMap<>();

		Class<?>[] classes = new Class[] {
				IAgntsErr.class,
				IBsafeErr.class,
				IClErr.class,
				IDbdrvErr.class,
				IDirErr.class,
				IEventErr.class,
				IFtErr.class,
				IHtmlErr.class,
				IMiscErr.class,
				INetErr.class,
				INifErr.class,
				INsfErr.class,
				IOdsErr.class,
				IOsErr.class,
				IRegErr.class,
				IRouteErr.class,
				ISecErr.class,
				ISrvErr.class,
				IXmlErr.class
		};

		for (Class<?> currClass : classes) {
			Field[] fields = currClass.getFields();
			for (Field currField : fields) {
				ErrorText currTxt = currField.getAnnotation(ErrorText.class);
				if (currTxt!=null) {
					try {
						short currCode = currField.getShort(currClass);
						texts.put(currCode, currTxt.text());
					} catch (IllegalArgumentException | IllegalAccessException e) {
						continue;
					}
				}
			}
		}

		return texts;
	}

}
