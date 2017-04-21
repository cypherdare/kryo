/* Copyright (c) 2008, Nathan Sweet
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.esotericsoftware.kryo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.junit.Assert;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;

public class TaggedFieldSerializerTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	public void testTaggedFieldSerializer () throws FileNotFoundException {
		TestClass object1 = new TestClass();
		object1.moo = 2;
		object1.child = new TestClass();
		object1.child.moo = 5;
		object1.other = new AnotherClass();
		object1.other.value = "meow";
		object1.ignored = 32;
		kryo.setDefaultSerializer(TaggedFieldSerializer.class);
		kryo.register(TestClass.class);
		kryo.register(AnotherClass.class);
		TestClass object2 = roundTrip(57, 75, object1);
		assertTrue(object2.ignored == 0);
	}

	public void testAddedField () throws FileNotFoundException {
		TestClass object1 = new TestClass();
		object1.child = new TestClass();
		object1.other = new AnotherClass();
		object1.other.value = "meow";

		TaggedFieldSerializer serializer = new TaggedFieldSerializer(kryo, TestClass.class);
		serializer.removeField("text");
		kryo.register(TestClass.class, serializer);
		kryo.register(AnotherClass.class, new TaggedFieldSerializer(kryo, AnotherClass.class));
		roundTrip(39, 55, object1);

		kryo.register(TestClass.class, new TaggedFieldSerializer(kryo, TestClass.class));
		Object object2 = kryo.readClassAndObject(input);
		assertEquals(object1, object2);
	}

	/** Serializes an array with a Class with two tagged fields. Then deserializes it using a second Kryo instance that
	 * has registered a mock past version of the class at the same ID. An array is used to ensure subsequent bytes in
	 * the stream are unaffected.*/
	public void testForwardCompatibility () {
		FutureClass futureObject = new FutureClass();
		futureObject.value = 3;
		futureObject.futureString = "future";
		Object[] futureArray = new Object[2];
		futureArray[0] = futureObject;
		futureArray[1] = new TestClass();

		kryo.setDefaultSerializer(TaggedFieldSerializer.class);
		kryo.getTaggedFieldSerializerConfig().setIgnoreUnknownTags(true);
		kryo.register(TestClass.class);
		kryo.register(Object[].class);
		TaggedFieldSerializer futureSerializer = new TaggedFieldSerializer(kryo, FutureClass.class);
		futureSerializer.setIgnoreUnknownTags(true);
		kryo.register(FutureClass.class, futureSerializer);
		roundTrip(12, 15, futureObject);
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		output = new Output(outStream);
		kryo.writeClassAndObject(output, futureObject);
		output.flush();
		byte[] futureSingleInstanceData = outStream.toByteArray();

		outStream = new ByteArrayOutputStream();
		output = new Output(outStream);
		kryo.writeClassAndObject(output, futureArray);
		output.flush();
		byte[] futureArrayData = outStream.toByteArray();

		TaggedFieldSerializer presentSerializer = new TaggedFieldSerializer(kryo, FutureClass.class);
		presentSerializer.setIgnoreUnknownTags(true);
		presentSerializer.removeField("futureString"); // simulate past version of application
		kryo.register(FutureClass.class, presentSerializer);
		ByteArrayInputStream inStream = new ByteArrayInputStream(futureSingleInstanceData);
		input = new Input(inStream);
		FutureClass presentObject = (FutureClass)kryo.readClassAndObject(input);
		assertEquals(futureObject.value, presentObject.value); // passes because the missing tag is the last sequentially
		Assert.assertNotEquals(futureObject.futureString, presentObject.futureString);

		inStream = new ByteArrayInputStream(futureArrayData);
		input = new Input(inStream);
		Object[] presentArray = (Object[])kryo.readClassAndObject(input); // fails because objects following future class in stream are read too early
		presentObject = (FutureClass)presentArray[0];
		assertEquals(futureObject.value, presentObject.value);
		assertEquals(futureArray[1], presentArray[1]);
	}

	/** Attempts to register a class with a field tagged with a value already used in its superclass. Should receive
	 * IllegalArgumentException. */
	public void testInvalidTagValue () {
		Kryo newKryo = new Kryo();
		newKryo.setReferences(true);
		newKryo.getTaggedFieldSerializerConfig().setIgnoreUnknownTags(true);
		newKryo.setDefaultSerializer(TaggedFieldSerializer.class);

		boolean receivedIAE = false;
		try {
			newKryo.register(IncompatibleClass.class);
		} catch (IllegalArgumentException e){
			receivedIAE = true;
		}
		assertTrue(receivedIAE);
	}

	static public class TestClass {
		@Tag(0) public String text = "something";
		@Tag(1) public int moo = 120;
		@Tag(2) public long moo2 = 1234120;
		@Tag(3) public TestClass child;
		@Tag(4) public int zzz = 123;
		@Tag(5) public AnotherClass other;
		@Tag(6) @Deprecated public int ignored;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			TestClass other = (TestClass)obj;
			if (child == null) {
				if (other.child != null) return false;
			} else if (!child.equals(other.child)) return false;
			if (moo != other.moo) return false;
			if (moo2 != other.moo2) return false;
			if (text == null) {
				if (other.text != null) return false;
			} else if (!text.equals(other.text)) return false;
			if (zzz != other.zzz) return false;
			return true;
		}
	}

	static public class IncompatibleClass extends TestClass {
		@Tag(3) public int poorlyTaggedField = 5;
	}

	static public class AnotherClass {
		@Tag(1) String value;
	}

	private static class FutureClass {
		@Tag(0) public Integer value;
		@Tag(1) public String futureString = "unchanged";

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			FutureClass that = (FutureClass) o;
			if (value != null ? !value.equals(that.value) : that.value != null) return false;
			return futureString != null ? futureString.equals(that.futureString) : that.futureString == null;
		}
	}
}
