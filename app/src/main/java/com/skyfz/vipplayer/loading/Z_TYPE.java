package com.skyfz.vipplayer.loading;

import com.skyfz.vipplayer.loading.components.LeafBuilder;

/**
 * Created by guoni on 2017/11/4.
 */

public enum Z_TYPE{
    LEAF_ROTATE(LeafBuilder.class),
    ;

    private final Class<?> mBuilderClass;

    Z_TYPE(Class<?> builderClass)
    {
        this.mBuilderClass = builderClass;
    }

    <T extends ZLoadingBuilder>T newInstance(){
        try
        {
            return (T) mBuilderClass.newInstance();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
