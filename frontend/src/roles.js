// Role-specific routes and navigation, taken from the SRS user roles (Section 2.2).
export const ROLE_HOME = { TENANT: '/tenant', OWNER: '/owner', ADMIN: '/admin' };
export const ROLE_LABEL = { TENANT: 'Tenant', OWNER: 'Owner', ADMIN: 'Admin' };

// Items without "to" are features planned for later increments and are shown as disabled.
export const ROLE_NAV = {
  TENANT: [
    { label: 'Dashboard', to: '/tenant' },
    { label: 'Search Listings' },
    { label: 'Favorites' },
    { label: 'Messages' },
    { label: 'My Bookings' },
  ],
  OWNER: [
    { label: 'Dashboard', to: '/owner' },
    { label: 'My Listings' },
    { label: 'Rooms & Availability' },
    { label: 'Messages' },
    { label: 'Booking Requests' },
  ],
  ADMIN: [
    { label: 'Dashboard', to: '/admin' },
    { label: 'Verification Queue' },
    { label: 'Users' },
  ],
};
