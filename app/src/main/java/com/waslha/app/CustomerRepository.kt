package com.waslha.app

class CustomerRepository(private val api: WaslhaApi = ApiProvider.api) {
    suspend fun profile(): Result<CustomerProfileDto> = runCatching {
        val response = api.customerMe()
        require(response.success && response.data != null) { response.message ?: "تعذر تحميل بيانات الحساب" }
        response.data
    }

    suspend fun updateProfile(request: CustomerProfileUpdateRequest): Result<CustomerProfileDto> = runCatching {
        val response = api.updateCustomer(request)
        require(response.success && response.data != null) { response.message ?: "تعذر حفظ بيانات الحساب" }
        response.data
    }

    suspend fun wallet(): Result<WalletDto> = runCatching {
        val response = api.wallet()
        require(response.success && response.data != null) { response.message ?: "تعذر تحميل رصيد المحفظة" }
        response.data
    }

    suspend fun savedPlaces(): Result<List<SavedPlaceDto>> = runCatching {
        val response = api.savedPlaces()
        require(response.success && response.data != null) { response.message ?: "تعذر تحميل الأماكن المحفوظة" }
        response.data
    }

    suspend fun savePlace(slot: String, request: SavedPlaceRequest): Result<SavedPlaceDto> = runCatching {
        val response = api.savePlace(slot, request)
        require(response.success && response.data != null) { response.message ?: "تعذر حفظ المكان" }
        response.data
    }

    suspend fun deletePlace(id: String): Result<Boolean> = runCatching {
        val response = api.deletePlace(id)
        require(response.success && response.data != null) { response.message ?: "تعذر حذف المكان" }
        response.data.deleted
    }

    suspend fun deleteAccount(): Result<Boolean> = runCatching {
        val response = api.deleteCustomer()
        require(response.success && response.data != null) { response.message ?: "تعذر حذف الحساب" }
        response.data.deleted
    }

    suspend fun nearbyDrivers(vehicleType: String? = null): Result<List<NearbyDriverDto>> = runCatching {
        val response = api.nearbyDrivers(vehicleType)
        require(response.success && response.data != null) { response.message ?: "تعذر تحميل الكباتن القريبين" }
        response.data
    }
}
