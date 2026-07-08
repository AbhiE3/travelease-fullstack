export type TripStatus = 'PLANNING' | 'CONFIRMED' | 'ONGOING' | 'COMPLETED' | 'CANCELLED';

export interface TripOrganizer {
  userId: string;
  name: string;
  email: string;
}

export interface Trip {
  tripId: string;
  tripName: string;
  organizer: TripOrganizer;
  sourceLocation: string;
  destinationId: number;
  budgetAmount: number;
  categoryId: number;
  startDate: string;
  endDate: string;
  status: TripStatus;
  viewerRole: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTripPayload {
  tripName: string;
  sourceLocation: string;
  destinationId: number;
  budgetAmount: number;
  categoryId: number;
  startDate: string;
  endDate: string;
}

export type TripMemberStatus = 'INVITED' | 'ACCEPTED' | 'REJECTED';

export interface TripMember {
  tripMemberId: string;
  userId: string;
  name: string;
  email: string;
  memberStatus: TripMemberStatus;
  joinedDate: string;
  budgetAmount: number;
  spentAmount: number;
}

export interface PendingInvitation {
  tripMemberId: string;
  tripId: string;
  tripName: string;
  organizer: TripOrganizer;
  sourceLocation: string;
  startDate: string;
  endDate: string;
  memberStatus: TripMemberStatus;
}
