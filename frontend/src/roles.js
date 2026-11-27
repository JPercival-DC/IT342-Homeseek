// Role-specific routes and navigation, taken from the SRS user roles (Section 2.2).
export const ROLE_HOME = { TENANT: '/tenant', OWNER: '/owner', ADMIN: '/admin' };
export const ROLE_LABEL = { TENANT: 'Tenant', OWNER: 'Owner', ADMIN: 'Admin' };

// Items without "to" are features planned for later increments and are shown as disabled.
// Added to every role (Increment 2).
const ACCOUNT_NAV = [{ label: 'My Profile', to: '/profile' }, { label: 'Change Password', to: '/change-password' }];

export const ROLE_NAV = {
  TENANT: [
    { label: 'Dashboard', to: '/tenant' },
    { label: 'Search Listings' },
    { label: 'Favorites' },
    { label: 'Messages' },
    { label: 'My Bookings' },
    ...ACCOUNT_NAV,
  ],
  OWNER: [
    { label: 'Dashboard', to: '/owner' },
    { label: 'My Listings' },
    { label: 'Rooms & Availability' },
    { label: 'Messages' },
    { label: 'Booking Requests' },
    ...ACCOUNT_NAV,
  ],
  ADMIN: [
    { label: 'Dashboard', to: '/admin' },
    { label: 'Verification Queue' },
    { label: 'Users' },
    ...ACCOUNT_NAV,
  ],
};
