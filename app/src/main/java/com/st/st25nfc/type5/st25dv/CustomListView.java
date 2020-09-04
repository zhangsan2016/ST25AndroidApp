/*
  * @author STMicroelectronics MMY Application team
  *
  ******************************************************************************
  * @attention
  *
  * <h2><center>&copy; COPYRIGHT 2017 STMicroelectronics</center></h2>
  *
  * Licensed under ST MIX_MYLIBERTY SOFTWARE LICENSE AGREEMENT (the "License");
  * You may not use this file except in compliance with the License.
  * You may obtain a copy of the License at:
  *
  *        http://www.st.com/Mix_MyLiberty
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied,
  * AND SPECIFICALLY DISCLAIMING THE IMPLIED WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  ******************************************************************************
*/


package com.st.st25nfc.type5.st25dv;


import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ListView;

public class CustomListView extends ListView {

        private android.view.ViewGroup.LayoutParams params;
        private int oldCount = 0;

        public CustomListView(Context context, AttributeSet attrs)
        {
            super(context, attrs);
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            if (getCount() != oldCount)
            {
                int height = getChildAt(0).getHeight() + 1 ;
                oldCount = getCount();
                params = getLayoutParams();
                params.height = getCount() * height;
                setLayoutParams(params);
            }

            super.onDraw(canvas);
        }

    }

