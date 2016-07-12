/* Copyright (c) 2016, Martin Grotzke
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

package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;

import static com.esotericsoftware.minlog.Log.TRACE;
import static com.esotericsoftware.minlog.Log.trace;

/** Configuration for FieldSerializer instances. To configure defaults for new FieldSerializer instances
 * use {@link Kryo#getFieldSerializerConfig()}, to configure a specific FieldSerializer instance use setters
 * for configuration settings on this specific FieldSerializer. */
public class FieldSerializerConfig implements Cloneable {
    private boolean fieldsCanBeNull = true, setFieldsAsAccessible = true;
    private boolean ignoreSyntheticFields = true;
    private boolean fixedFieldTypes;
    /** If set, ASM-backend is used. Otherwise Unsafe-based backend or reflection is used */
    private boolean useAsm;
    /** If set, transient fields will be copied */
    private boolean copyTransient = true;
    /** If set, transient fields will be serialized */
    private boolean serializeTransient = false;
    private boolean forwardCompatibleTaggedFields = true;

    {
        useAsm = !FieldSerializer.unsafeAvailable;
        if (TRACE) trace("kryo.FieldSerializerConfig", "useAsm: " + useAsm);
    }

    @Override
    protected FieldSerializerConfig clone() {
        // clone is ok here as we have only primitive fields
        try { return (FieldSerializerConfig) super.clone(); }
        catch (CloneNotSupportedException e) { throw new RuntimeException(e); }
    }

    /** Sets the default value for {@link FieldSerializer.CachedField#setCanBeNull(boolean)}.
     * @param fieldsCanBeNull False if none of the fields are null. Saves 0-1 byte per field. True if it is not known (default). */
    public void setFieldsCanBeNull (boolean fieldsCanBeNull) {
        this.fieldsCanBeNull = fieldsCanBeNull;
        if (TRACE) trace("kryo.FieldSerializerConfig", "setFieldsCanBeNull: " + fieldsCanBeNull);
    }

    /** Controls which fields are serialized.
     * @param setFieldsAsAccessible If true, all non-transient fields (inlcuding private fields) will be serialized and
	 *                              {@link java.lang.reflect.Field#setAccessible(boolean) set as accessible} if necessary (default).
	 *                              If false, only fields in the public API will be serialized. */
    public void setFieldsAsAccessible (boolean setFieldsAsAccessible) {
        this.setFieldsAsAccessible = setFieldsAsAccessible;
        if (TRACE) trace("kryo.FieldSerializerConfig", "setFieldsAsAccessible: " + setFieldsAsAccessible);
    }

    /** Controls if synthetic fields are serialized. Default is true.
     * @param ignoreSyntheticFields If true, only non-synthetic fields will be serialized. */
    public void setIgnoreSyntheticFields (boolean ignoreSyntheticFields) {
        this.ignoreSyntheticFields = ignoreSyntheticFields;
        if (TRACE) trace("kryo.FieldSerializerConfig", "setIgnoreSyntheticFields: " + ignoreSyntheticFields);
    }

    /** Sets the default value for {@link FieldSerializer.CachedField#setClass(Class)} to the field's declared type. This allows FieldSerializer to
     * be more efficient, since it knows field values will not be a subclass of their declared type. Default is false. */
    public void setFixedFieldTypes (boolean fixedFieldTypes) {
        this.fixedFieldTypes = fixedFieldTypes;
        if (TRACE) trace("kryo.FieldSerializerConfig", "setFixedFieldTypes: " + fixedFieldTypes);
    }

    /** Controls whether ASM should be used.
     * @param setUseAsm If true, ASM will be used for fast serialization. If false, Unsafe will be used (default) */
    public void setUseAsm (boolean setUseAsm) {
        useAsm = setUseAsm;
        if (!useAsm && !FieldSerializer.unsafeAvailable) {
            useAsm = true;
            if (TRACE) trace("kryo.FieldSerializerConfig", "sun.misc.Unsafe is unavailable, using ASM.");
        }
        if (TRACE) trace("kryo.FieldSerializerConfig", "setUseAsm: " + setUseAsm);
    }

    /** If false, when {@link Kryo#copy(Object)} is called all transient fields that are accessible will be ignored from
     * being copied. This has to be set before registering classes with kryo for it to be used by all field
     * serializers. If transient fields has to be copied for specific classes then use {@link FieldSerializer#setCopyTransient(boolean)}.
     * Default is true.
     */
    public void setCopyTransient (boolean setCopyTransient) {
        copyTransient = setCopyTransient;
    }

    /**
     * If set, transient fields will be serialized
     * Default is false
     * @param serializeTransient
     */
    public void setSerializeTransient(boolean serializeTransient) {
        this.serializeTransient = serializeTransient;
    }

    /** If false, {@link TaggedFieldSerializer#read(Kryo, Input, Class) TaggedFieldSerializer.read()} will throw a
     * {@link com.esotericsoftware.kryo.KryoException KryoException} when encountering unknown tags (legacy behavior)
     * rather than assuming a future {@link TaggedFieldSerializer.Late @Late} tagged field. This setting does not affect
     * write behavior--fields marked Late will still use chunked encoding and can still be read by current and later
     * versions. This setting applies only to TaggedFieldSerializer. */
    public void setForwardCompatibleTaggedFields (boolean forwardCompatible){
        this.forwardCompatibleTaggedFields = forwardCompatible;
    }

    public boolean isFieldsCanBeNull() {
        return fieldsCanBeNull;
    }

    public boolean isSetFieldsAsAccessible() {
        return setFieldsAsAccessible;
    }

    public boolean isIgnoreSyntheticFields() {
        return ignoreSyntheticFields;
    }

    public boolean isFixedFieldTypes() {
        return fixedFieldTypes;
    }

    public boolean isUseAsm() {
        return useAsm;
    }

    public boolean isCopyTransient() {
        return copyTransient;
    }

    public boolean isSerializeTransient() {
        return serializeTransient;
    }

    public boolean isForwardCompatibleTaggedFields() {
        return forwardCompatibleTaggedFields;
    }
}
