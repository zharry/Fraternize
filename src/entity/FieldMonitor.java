// Jacky Liao and Harry Zhang
// Jan 18, 2017
// Summative
// ICS4U Ms.Strelkovska

package entity;

import util.Util;
import util.math.Quat4;
import util.math.Vec3;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

// A monitor for all the fields in a player, in order to keep them synchronized
public class FieldMonitor {

	// Serializer and deserializer for a field
	private static class Serializer {
		private interface Serialize {
			void serialize(ByteBuffer b, Object value);
		}
		private interface Deserialize {
			Object deserialize(ByteBuffer b);
		}
		private Serialize serialize;
		private Deserialize deserialize;
		private Serializer(Serialize serialize, Deserialize deserialize) {
			this.serialize = serialize;
			this.deserialize = deserialize;
		}
	}

	// A list of serializers for a field
	private static HashMap<Class<?>, Serializer> serializers = new HashMap<>();

	// Add a new serializer
	private static void putSerializer(Class<?> clazz, Serializer.Serialize serialize, Serializer.Deserialize deserialize) {
		serializers.put(clazz, new Serializer(serialize, deserialize));
	}

	static {
		// Add serializers for all primitives
		putSerializer(byte.class, (b, v) -> b.put((byte) v), ByteBuffer::get);
		putSerializer(short.class, (b, v) -> b.putShort((short) v), ByteBuffer::getShort);
		putSerializer(int.class, (b, v) -> b.putInt((int) v), ByteBuffer::getInt);
		putSerializer(long.class, (b, v) -> b.putLong((long) v), ByteBuffer::getLong);
		putSerializer(float.class, (b, v) -> b.putFloat((float) v), ByteBuffer::getFloat);
		putSerializer(double.class, (b, v) -> b.putDouble((double) v), ByteBuffer::getDouble);
		putSerializer(boolean.class, (b, v) -> b.put((boolean) v ? (byte) 1 : 0), b -> b.get() != 0);
		// String
		putSerializer(String.class, (b, v) -> {
			String s = (String) v;
			byte[] bs = Util.getBytes(s);
			b.putInt(bs.length).put(bs);
		}, b -> {
			int len = b.getInt();
			byte[] bs = new byte[len];
			b.get(bs);
			return Util.getString(bs);
		});
		// Vec3
		putSerializer(Vec3.class, (b, v) -> {
			Vec3 vv = (Vec3) v;
			b.putDouble(vv.x).putDouble(vv.y).putDouble(vv.z);
		}, b -> new Vec3(b.getDouble(), b.getDouble(), b.getDouble()));
		// Quat4
		putSerializer(Quat4.class, (b, v) -> {
			Quat4 qq = (Quat4) v;
			b.putDouble(qq.w).putDouble(qq.x).putDouble(qq.y).putDouble(qq.z);
		}, b -> new Quat4(b.getDouble(), b.getDouble(), b.getDouble(), b.getDouble()));
	}

	// Information about a field
	private static class Info {
		private static class FieldValue {
			private Field field;
			private Object value;
			private Serializer serializer;
			private FieldValue(Field field, Serializer serializer) {
				this.field = field;
				this.serializer = serializer;
			}
		}
		private ArrayList<FieldValue> fields = new ArrayList<>();
	}

	// Information for a list of fields
	private static HashMap<Class<? extends Entity>, Info> infoCache = new HashMap<>();

	// Attempt to track a field
	private Info track(Class<? extends Entity> clazz) {
		Info info = new Info();
		for(Field field : clazz.getFields()) {
			// Only track a field if it has the annotation @Synchronize
			Annotation annotation = field.getAnnotation(Synchronize.class);
			if(annotation != null) {
				info.fields.add(new Info.FieldValue(field, serializers.get(field.getType())));
			}
		}
		infoCache.put(clazz, info);
		return info;
	}

	// List of all field values
	private ArrayList<Info.FieldValue> fieldValues = new ArrayList<>();

	// The entity to track
	private Entity entity;

	// Initialize a field monitor for an entity
	public FieldMonitor(Entity e) {
		entity = e;
		Class<? extends Entity> clazz = e.getClass();
		Info info = infoCache.get(clazz);
		if(info == null)
			info = track(e.getClass());
		try {
			// Go through all the trackable fields
			for(Info.FieldValue fv : info.fields) {
				// Track them
				Info.FieldValue newFv = new Info.FieldValue(fv.field, fv.serializer);
				newFv.value = newFv.field.get(e);
				fieldValues.add(newFv);
			}
		} catch(IllegalAccessException ex) {
			throw new RuntimeException(ex);
		}
	}

	// Serialize the object
	public void serialize(ByteBuffer b, boolean force) {
		for(int i = 0; i < fieldValues.size(); ++i) {
			Info.FieldValue fv = fieldValues.get(i);
			try {
				Object val = fv.field.get(entity);
				if(val == null && (fv.value != null || force)) {
					b.putInt(-i);
				} else if(val != null) {
					if(!val.equals(fv.value) || force) {
						b.putInt(i);
						fv.serializer.serialize.serialize(b, val);
					}
				}
				fv.value = val;
			} catch(IllegalAccessException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	// Deserialize the object
	public void deserialize(ByteBuffer b) {
		while(b.hasRemaining()) {
			int field = b.getInt();
			boolean nullify = false;
			if(field < 0) {
				nullify = true;
				field = -field;
			}
			Info.FieldValue fv = fieldValues.get(field);
			try {
				if(nullify) {
					fv.value = null;
					fv.field.set(entity, null);
					continue;
				}
				fv.value = serializers.get(fv.field.getType()).deserialize.deserialize(b);
				fv.field.set(entity, fv.value);
			} catch(IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
