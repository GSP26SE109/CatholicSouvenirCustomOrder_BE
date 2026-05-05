# Frontend Integration Example - AI Product Description

## API Endpoint
```
POST /api/product/generate-description
Content-Type: application/json
```

## Complete React Example

### 1. API Service
```typescript
// services/productService.ts
export interface GenerateDescriptionRequest {
  productName: string;
  category?: string;
  tags?: string;
  existingDescription?: string;
}

export interface GenerateDescriptionResponse {
  description: string;
  aiGenerated: boolean;
  message: string;
}

export const generateProductDescription = async (
  request: GenerateDescriptionRequest
): Promise<GenerateDescriptionResponse> => {
  const response = await fetch('/api/product/generate-description', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw new Error('Failed to generate description');
  }

  const result = await response.json();
  return result.data;
};
```

### 2. React Component with Hook
```typescript
// hooks/useAIDescription.ts
import { useState } from 'react';
import { generateProductDescription } from '../services/productService';

export const useAIDescription = () => {
  const [isGenerating, setIsGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const generate = async (
    productName: string,
    category?: string,
    tags?: string[],
    existingDescription?: string
  ) => {
    if (!productName.trim()) {
      setError('Product name is required');
      return null;
    }

    setIsGenerating(true);
    setError(null);

    try {
      const result = await generateProductDescription({
        productName,
        category,
        tags: tags?.join(', '),
        existingDescription,
      });

      return result;
    } catch (err) {
      setError('Failed to generate description. Please try again.');
      console.error('AI Description Error:', err);
      return null;
    } finally {
      setIsGenerating(false);
    }
  };

  return { generate, isGenerating, error };
};
```

### 3. Form Component
```typescript
// components/CreateProductForm.tsx
import React, { useState } from 'react';
import { useAIDescription } from '../hooks/useAIDescription';

interface ProductFormData {
  productName: string;
  productDescription: string;
  categoryId: string;
  categoryName: string;
  tags: string[];
  productPrice: number;
  quantity: number;
  size: string;
  images: File[];
}

export const CreateProductForm: React.FC = () => {
  const [formData, setFormData] = useState<ProductFormData>({
    productName: '',
    productDescription: '',
    categoryId: '',
    categoryName: '',
    tags: [],
    productPrice: 0,
    quantity: 0,
    size: '',
    images: [],
  });

  const { generate, isGenerating, error } = useAIDescription();
  const [showAINotification, setShowAINotification] = useState(false);

  const handleGenerateDescription = async () => {
    const result = await generate(
      formData.productName,
      formData.categoryName,
      formData.tags,
      formData.productDescription
    );

    if (result) {
      setFormData(prev => ({
        ...prev,
        productDescription: result.description,
      }));

      // Show notification
      setShowAINotification(true);
      setTimeout(() => setShowAINotification(false), 3000);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    // Submit product creation...
  };

  return (
    <form onSubmit={handleSubmit} className="product-form">
      {/* Product Name */}
      <div className="form-group">
        <label htmlFor="productName">
          Product Name <span className="required">*</span>
        </label>
        <input
          id="productName"
          type="text"
          value={formData.productName}
          onChange={(e) => setFormData(prev => ({ 
            ...prev, 
            productName: e.target.value 
          }))}
          required
        />
      </div>

      {/* Category */}
      <div className="form-group">
        <label htmlFor="category">Category</label>
        <select
          id="category"
          value={formData.categoryId}
          onChange={(e) => {
            const selectedOption = e.target.selectedOptions[0];
            setFormData(prev => ({
              ...prev,
              categoryId: e.target.value,
              categoryName: selectedOption.text,
            }));
          }}
        >
          <option value="">Select category</option>
          <option value="uuid-1">Tượng Thánh</option>
          <option value="uuid-2">Đồ Cầu Nguyện</option>
          {/* ... more options */}
        </select>
      </div>

      {/* Tags */}
      <div className="form-group">
        <label>Tags</label>
        <input
          type="text"
          placeholder="Enter tags separated by comma"
          onChange={(e) => setFormData(prev => ({
            ...prev,
            tags: e.target.value.split(',').map(t => t.trim()),
          }))}
        />
      </div>

      {/* Product Description with AI Button */}
      <div className="form-group">
        <div className="label-with-button">
          <label htmlFor="description">Product Description</label>
          <button
            type="button"
            onClick={handleGenerateDescription}
            disabled={isGenerating || !formData.productName.trim()}
            className="btn-ai-generate"
            title={!formData.productName.trim() ? 'Please enter product name first' : ''}
          >
            {isGenerating ? (
              <>
                <span className="spinner" />
                Generating...
              </>
            ) : (
              <>
                <span className="ai-icon">🤖</span>
                Generate AI Description
              </>
            )}
          </button>
        </div>

        <textarea
          id="description"
          value={formData.productDescription}
          onChange={(e) => setFormData(prev => ({ 
            ...prev, 
            productDescription: e.target.value 
          }))}
          placeholder="Enter product description or use AI to generate"
          rows={6}
          className="description-textarea"
        />

        {error && (
          <div className="error-message">{error}</div>
        )}

        {showAINotification && (
          <div className="success-message">
            ✓ AI description generated successfully! You can edit it before submitting.
          </div>
        )}

        <small className="help-text">
          You can manually enter a description or use AI to generate one automatically
        </small>
      </div>

      {/* Other fields... */}
      
      <button type="submit" className="btn-submit">
        Create Product
      </button>
    </form>
  );
};
```

### 4. CSS Styling
```css
/* styles/ProductForm.css */
.form-group {
  margin-bottom: 1.5rem;
}

.label-with-button {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.label-with-button label {
  font-weight: 600;
  margin: 0;
}

.btn-ai-generate {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-ai-generate:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-ai-generate:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ai-icon {
  font-size: 1.2rem;
}

.spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.description-textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-family: inherit;
  font-size: 0.95rem;
  resize: vertical;
  transition: border-color 0.3s;
}

.description-textarea:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.error-message {
  margin-top: 0.5rem;
  padding: 0.5rem;
  background-color: #fee;
  color: #c33;
  border-radius: 4px;
  font-size: 0.875rem;
}

.success-message {
  margin-top: 0.5rem;
  padding: 0.5rem;
  background-color: #efe;
  color: #3a3;
  border-radius: 4px;
  font-size: 0.875rem;
}

.help-text {
  display: block;
  margin-top: 0.5rem;
  color: #666;
  font-size: 0.875rem;
}

.required {
  color: #e53e3e;
}
```

## Vue.js Example

```vue
<!-- components/CreateProductForm.vue -->
<template>
  <form @submit.prevent="handleSubmit" class="product-form">
    <!-- Product Name -->
    <div class="form-group">
      <label for="productName">
        Product Name <span class="required">*</span>
      </label>
      <input
        id="productName"
        v-model="formData.productName"
        type="text"
        required
      />
    </div>

    <!-- Product Description with AI -->
    <div class="form-group">
      <div class="label-with-button">
        <label for="description">Product Description</label>
        <button
          type="button"
          @click="handleGenerateDescription"
          :disabled="isGenerating || !formData.productName.trim()"
          class="btn-ai-generate"
        >
          <span v-if="isGenerating" class="spinner" />
          <span v-else class="ai-icon">🤖</span>
          {{ isGenerating ? 'Generating...' : 'Generate AI Description' }}
        </button>
      </div>

      <textarea
        id="description"
        v-model="formData.productDescription"
        placeholder="Enter product description or use AI to generate"
        rows="6"
        class="description-textarea"
      />

      <div v-if="error" class="error-message">{{ error }}</div>
      <div v-if="showSuccess" class="success-message">
        ✓ AI description generated successfully!
      </div>

      <small class="help-text">
        You can manually enter a description or use AI to generate one
      </small>
    </div>

    <!-- Other fields... -->

    <button type="submit" class="btn-submit">Create Product</button>
  </form>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';

const formData = reactive({
  productName: '',
  productDescription: '',
  categoryName: '',
  tags: [] as string[],
});

const isGenerating = ref(false);
const error = ref('');
const showSuccess = ref(false);

const handleGenerateDescription = async () => {
  if (!formData.productName.trim()) {
    error.value = 'Product name is required';
    return;
  }

  isGenerating.value = true;
  error.value = '';

  try {
    const response = await fetch('/api/product/generate-description', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        productName: formData.productName,
        category: formData.categoryName,
        tags: formData.tags.join(', '),
        existingDescription: formData.productDescription,
      }),
    });

    const result = await response.json();
    
    if (result.success) {
      formData.productDescription = result.data.description;
      showSuccess.value = true;
      setTimeout(() => showSuccess.value = false, 3000);
    }
  } catch (err) {
    error.value = 'Failed to generate description';
    console.error(err);
  } finally {
    isGenerating.value = false;
  }
};

const handleSubmit = () => {
  // Submit form...
};
</script>
```

## Testing

### Manual Test with cURL
```bash
curl -X POST http://localhost:8080/api/product/generate-description \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "Tượng Đức Mẹ Maria",
    "category": "Tượng Thánh",
    "tags": "Đức Mẹ, Maria, Tượng Thánh"
  }'
```

### Expected Response
```json
{
  "success": true,
  "message": "Mô tả được tạo bởi AI",
  "data": {
    "description": "Tượng Đức Mẹ Maria là một sản phẩm lưu niệm Công Giáo thuộc danh mục Tượng Thánh, được chế tác tỉ mỉ với tâm huyết và lòng sùng kính...",
    "aiGenerated": true,
    "message": "Mô tả được tạo bởi AI"
  }
}
```

## Best Practices

1. **Always validate productName** before calling API
2. **Show loading state** during generation (2-5 seconds)
3. **Allow editing** after AI generates description
4. **Handle errors gracefully** with user-friendly messages
5. **Provide fallback** if AI is unavailable
6. **Don't auto-submit** - let user review AI-generated content
7. **Add tooltips** to explain the AI feature
8. **Consider rate limiting** on frontend to prevent spam

## UX Recommendations

- ✅ Disable button when productName is empty
- ✅ Show spinner/loading state during generation
- ✅ Display success notification after generation
- ✅ Allow user to regenerate if not satisfied
- ✅ Keep manual input option always available
- ✅ Show character count for description field
- ✅ Add "Enhance with AI" option for existing descriptions
