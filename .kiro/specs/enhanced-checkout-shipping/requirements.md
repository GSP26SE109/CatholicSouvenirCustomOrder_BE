# Requirements Document

## Introduction

This feature enhances the checkout process by integrating real-time shipping fee calculation using GHN API, auto-filling customer shipping information from their profile, and storing complete shipping details in orders for seamless shipment creation by artisans. The feature follows the existing payment flow: Checkout → Payment → Shipment Creation.

## Glossary

- **CheckoutSystem**: The system component responsible for processing customer checkout requests (CheckoutServiceImp)
- **GHNService**: The Giao Hàng Nhanh (GHN) shipping service integration (ShippingServiceImp, GHNAddressService)
- **ShippingFeeCalculator**: Component that calculates shipping costs using GHN API (ShippingService.calculateShippingFee)
- **CustomerProfile**: User profile containing saved address information (UserProfile entity)
- **OrderEntity**: Database entity storing order information including shipping details (Order model)
- **ArtisanShipmentCreator**: Component allowing artisans to create shipments from orders (ShippingService.createShipment)
- **PaymentSystem**: Component handling payment processing (PaymentServiceImp)
- **OrderGroup**: Entity grouping multiple orders from different artisans for single payment

## Requirements

### Requirement 1

**User Story:** As a customer, I want my saved address information to be automatically filled in the checkout form, so that I don't have to re-enter my shipping details every time.

#### Acceptance Criteria

1. WHEN THE CustomerProfile SHALL retrieve the customer's saved address information from UserProfile
2. WHEN THE CheckoutSystem displays the checkout form, THE CheckoutSystem SHALL pre-populate shipping fields with CustomerProfile data
3. THE CheckoutSystem SHALL allow customers to edit pre-filled address information before checkout
4. WHEN THE CustomerProfile contains district and ward information, THE CheckoutSystem SHALL map these to GHN district ID and ward code
5. WHERE THE CustomerProfile lacks complete address data, THE CheckoutSystem SHALL display empty fields for manual entry

### Requirement 2

**User Story:** As a customer, I want to see the shipping fee calculated in real-time when I enter my delivery address, so that I know the total cost before completing my purchase.

#### Acceptance Criteria

1. THE CheckoutSystem SHALL provide an API endpoint to calculate shipping fee before checkout
2. WHEN THE customer enters province, district, ward, THE CheckoutSystem SHALL calculate shipping fee for cart items
3. THE ShippingFeeCalculator SHALL group cart items by artisan and calculate separate shipping fees
4. THE ShippingFeeCalculator SHALL return total shipping fee and breakdown by artisan
5. IF THE GHN API call fails, THE CheckoutSystem SHALL return a default shipping fee of 30,000 VND per artisan

### Requirement 3

**User Story:** As a customer, I want to select my delivery address using province/district/ward dropdowns with GHN data, so that my address is valid for shipping.

#### Acceptance Criteria

1. THE CheckoutSystem SHALL provide cascading dropdowns for province, district, and ward selection
2. WHEN THE customer selects a province, THE CheckoutSystem SHALL load districts for that province from GHN API
3. WHEN THE customer selects a district, THE CheckoutSystem SHALL load wards for that district from GHN API
4. THE CheckoutSystem SHALL store both the display names and GHN IDs/codes for selected locations
5. THE CheckoutSystem SHALL validate that district ID and ward code are provided before checkout

### Requirement 4

**User Story:** As a system, I want to store complete shipping information in the Order entity, so that artisans can create shipments without requesting additional information.

#### Acceptance Criteria

1. WHEN THE CheckoutSystem creates an order, THE OrderEntity SHALL store recipient name, phone, and full address
2. THE OrderEntity SHALL store GHN district ID and ward code for shipping integration
3. THE OrderEntity SHALL store calculated shipping fee amount
4. THE OrderEntity SHALL store package dimensions and weight
5. THE OrderEntity SHALL store customer notes for delivery

### Requirement 5

**User Story:** As an artisan, I want to create a shipment directly from an order with pre-filled information, so that I can quickly process shipping without manual data entry.

#### Acceptance Criteria

1. WHEN THE ArtisanShipmentCreator accesses an order, THE ArtisanShipmentCreator SHALL retrieve all shipping information from OrderEntity
2. THE ArtisanShipmentCreator SHALL map OrderEntity shipping data to CreateShipmentRequest automatically
3. THE ArtisanShipmentCreator SHALL allow artisans to review and modify shipping information before creating shipment
4. WHEN THE artisan confirms shipment creation, THE ArtisanShipmentCreator SHALL call GHN API with pre-filled data
5. THE ArtisanShipmentCreator SHALL link the created Shipment entity to the Order

### Requirement 6

**User Story:** As a customer, I want to see a breakdown of costs including product total, shipping fee, and grand total, so that I understand what I'm paying for.

#### Acceptance Criteria

1. THE CheckoutSystem SHALL display product subtotal separately from shipping fee
2. THE CheckoutSystem SHALL display shipping fee as a line item in the order summary
3. THE CheckoutSystem SHALL calculate and display grand total as product total plus shipping fee
4. WHEN THE shipping address changes, THE CheckoutSystem SHALL recalculate shipping fee and update totals
5. THE CheckoutSystem SHALL display all amounts in Vietnamese Dong (VND) format

### Requirement 7

**User Story:** As a system, I want to handle multiple orders from different artisans with individual shipping fees, so that each artisan's order has accurate shipping costs.

#### Acceptance Criteria

1. WHEN THE cart contains products from multiple artisans, THE CheckoutSystem SHALL calculate shipping fee for each artisan's order separately
2. THE CheckoutSystem SHALL sum all individual shipping fees for the total shipping cost
3. THE OrderEntity SHALL store the shipping fee specific to that artisan's order
4. THE CheckoutSystem SHALL display shipping fee breakdown by artisan in the checkout summary
5. WHEN THE payment is completed, THE CheckoutSystem SHALL include shipping fees in the total amount charged

### Requirement 8

**User Story:** As a customer, I want to provide shipping information before payment, so that the total cost including shipping is calculated accurately before I pay.

#### Acceptance Criteria

1. THE CheckoutSystem SHALL provide API to calculate shipping fee from cart before checkout
2. THE CheckoutSystem SHALL accept shipping information (recipient name, phone, address, district ID, ward code) in checkout request
3. WHEN THE customer submits checkout with shipping info, THE CheckoutSystem SHALL create orders with shipping details stored
4. THE CheckoutSystem SHALL create OrderGroup with total amount including product costs and shipping fees
5. THE CheckoutSystem SHALL return orderGroupId for payment initiation

### Requirement 10

**User Story:** As a frontend developer, I want a clear API flow to calculate shipping, checkout, and pay, so that I can implement the checkout process correctly.

#### Acceptance Criteria

1. THE CheckoutSystem SHALL provide GET /api/checkout/calculate-shipping endpoint accepting customerId and shipping address
2. THE endpoint SHALL return shipping fee breakdown by artisan and total shipping fee
3. THE CheckoutSystem SHALL accept enhanced CheckoutRequest with shipping details (recipientName, recipientPhone, deliveryAddress, toDistrictId, toWardCode)
4. WHEN THE checkout API is called, THE CheckoutSystem SHALL store shipping info in Order entities
5. THE CheckoutSystem SHALL include shipping fees in OrderGroup total amount for payment

### Requirement 9

**User Story:** As a system, I want to follow the correct flow: Checkout (with shipping info) → Payment → Shipment Creation, so that orders are processed in the right sequence.

#### Acceptance Criteria

1. THE CheckoutSystem SHALL collect shipping information and create orders with status PENDING
2. WHEN THE orders are created, THE CheckoutSystem SHALL return OrderGroup ID for payment
3. THE PaymentSystem SHALL process payment for the OrderGroup including shipping fees
4. WHEN THE payment succeeds, THE PaymentSystem SHALL update order status to PAID
5. WHEN THE order is PAID, THE ArtisanShipmentCreator SHALL allow artisan to create shipment using stored shipping information
