/*
 * Copyright (c) 2010-2012 LinkedIn, Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package krati.core.array.basic;

import java.util.Arrays;

import krati.array.Array;
import krati.array.DynamicArray;
import krati.array.ShortArray;

/**
 * MemoryShortArray is not thread safe.
 * 
 * @author jwu
 * 
 */
public class MemoryShortArray implements ShortArray, DynamicArray {
    protected short[][] _subArrays;
    protected final int _subArrayBits;
    protected final int _subArraySize;
    protected final int _subArrayMask;
    protected final boolean _autoExpand;
    
    public MemoryShortArray() {
        this(DynamicConstants.SUB_ARRAY_BITS, true);
    }
    
    public MemoryShortArray(int subArrayBits) {
        this(subArrayBits, true);
    }
    
    public MemoryShortArray(int subArrayBits, boolean autoExpand) {
        this._subArrayBits = subArrayBits;           // e.g. 16
        this._subArraySize = 1 << subArrayBits;      // e.g. 65536
        this._subArrayMask = this._subArraySize - 1; // e.g. 65535
        this._subArrays = new short[1][_subArraySize];
        this._autoExpand = autoExpand;
    }
    
    @Override
    public void clear() {
        for (short[] subArray : _subArrays) {
            Arrays.fill(subArray, (short) 0);
        }
    }
    
    /**
     * @return the current length of this Array
     */
    @Override
    public int length() {
        long len = _subArrays.length * (long)_subArraySize;
        return (len < Integer.MAX_VALUE) ? (int)len : Integer.MAX_VALUE;
    }
    
    /**
     * @return a boolean indicating an index is in the current range of this Array.
     */
    @Override
    public boolean hasIndex(int index) {
        return (index < 0) ? false : (index >> _subArrayBits) < _subArrays.length;
    }
    
    @Override
    public short get(int index) {
        if (index < 0) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        
        int subInd = index >> _subArrayBits;
        int offset = index & _subArrayMask;
        
        return _subArrays[subInd][offset];
    }
    
    public void set(int index, short value) {
        if (index < 0) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        
        int subInd = index >> _subArrayBits;
        int offset = index & _subArrayMask;
        
        // Expand array capacity automatically
        if (subInd >= _subArrays.length && _autoExpand) {
            expandCapacity(index);
        }
        
        _subArrays[subInd][offset] = value;
    }
    
    @Override
    public void set(int index, short value, long scn) {
        set(index, value);
    }
    
    @Override
    public void expandCapacity(int index) {
        if(index < 0) return;
        
        int numSubArrays = (index >> _subArrayBits) + 1;
        if (numSubArrays <= _subArrays.length) {
            return; // No need to expand this array
        }
        
        short[][] tmpArrays = new short[numSubArrays][];
        
        int i = 0;
        for (; i < _subArrays.length; i++) {
            tmpArrays[i] = _subArrays[i];
        }
        
        for (; i < numSubArrays; i++) {
            tmpArrays[i] = new short[_subArraySize];
        }
        
        _subArrays = tmpArrays;
        
        if(getArrayExpandListener() != null) {
            getArrayExpandListener().arrayExpanded(this);
        }
    }
    
    @Override
    public short[] getInternalArray() {
        int size = length();
        short[] result = new short[size];
        for (int i = 0; i < _subArrays.length; i++) {
            int len = Math.min(_subArraySize, size);
            System.arraycopy(_subArrays[i], 0, result, i * _subArraySize, len);
            size -= _subArraySize;
        }
        
        return result;
    }
    
    private ArrayExpandListener _expandListener;
    
    protected void setArrayExpandListener(ArrayExpandListener listener) {
        this._expandListener = listener;
    }
    
    protected ArrayExpandListener getArrayExpandListener() {
        return _expandListener;
    }
    
    @Override
    public final Array.Type getType() {
        return Array.Type.DYNAMIC;
    }
}
