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

import java.io.FileNotFoundException;

import org.objenesis.strategy.StdInstantiatorStrategy;

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

	static public class AnotherClass {
		@Tag(1) String value;
	}

	private static class Root {
		@TaggedFieldSerializer.Tag(0)
		private Integer b;
	}

	private static class RootWithNewDisorderedField {

		@TaggedFieldSerializer.Tag(0)
		private Integer b;

		/**
		 *  Because it starts with an 'a' kryo writes it before b field
		 */
		@TaggedFieldSerializer.Tag(1)
		private String a;

		public RootWithNewDisorderedField(Integer b, String a) {
			this.b = b;
			this.a = a;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof RootWithNewDisorderedField)) return false;

			RootWithNewDisorderedField that = (RootWithNewDisorderedField) o;

			if (b != null ? !b.equals(that.b) : that.b != null) return false;
			return a != null ? a.equals(that.a) : that.a == null;

		}

		@Override
		public int hashCode() {
			int result = b != null ? b.hashCode() : 0;
			result = 31 * result + (a != null ? a.hashCode() : 0);
			return result;
		}
	}

	private static class RootWithNewOrderedField {

		@TaggedFieldSerializer.Tag(0)
		private Integer b;

		/**
		 *  Because it starts with a 'c' kryo writes it after b field
		 */
		@TaggedFieldSerializer.Tag(1)
		private String c;

		public RootWithNewOrderedField(Integer b, String c) {
			this.b = b;
			this.c = c;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof RootWithNewOrderedField)) return false;

			RootWithNewOrderedField that = (RootWithNewOrderedField) o;

			if (b != null ? !b.equals(that.b) : that.b != null) return false;
			return c != null ? c.equals(that.c) : that.c == null;

		}

		@Override
		public int hashCode() {
			int result = b != null ? b.hashCode() : 0;
			result = 31 * result + (c != null ? c.hashCode() : 0);
			return result;
		}
	}
}
